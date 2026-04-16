from transformers import pipeline
try:
    import torch  
    _TORCH_AVAILABLE = True
except Exception: 
    torch = None
    _TORCH_AVAILABLE = False
import random
import hashlib
from transformers.utils.logging import set_verbosity_error
from typing import List, Tuple, Optional
from collections import OrderedDict
import logging
from app.config import Config

set_verbosity_error()
logger = logging.getLogger(__name__)

class LRUCache(OrderedDict):
    """LRU Cache with maximum size"""
    def __init__(self, maxsize=100):
        self.maxsize = maxsize
        super().__init__()

    def __setitem__(self, key, value):
        if key in self:
            self.move_to_end(key)
        super().__setitem__(key, value)
        if len(self) > self.maxsize:
            oldest = next(iter(self))
            del self[oldest]

class AIService:
    def __init__(self, cache_size=100):
        """Initialize AI service with lazy model loading"""
        self._translate_pipe = None
        self._qa_pipeline = None
        self.response_cache = LRUCache(maxsize=cache_size)
        self.conversation_patterns = {}
        logger.info("AI Service initialized (models will load on first use)")

    @property
    def translate_pipe(self):
        """Lazy load translation pipeline"""
        if self._translate_pipe is None:
            logger.info("Loading translation model...")
            self._translate_pipe = pipeline(
                "translation",
                model=Config.AI_MODEL_TRANSLATION,
            )
            logger.info("Translation model loaded")
        return self._translate_pipe

    @property
    def qa_pipeline(self):
        """Lazy load QA pipeline"""
        if self._qa_pipeline is None:
            logger.info("Loading QA model...")
            device = 0 if (torch is not None and hasattr(torch, "cuda") and torch.cuda.is_available()) else -1
            self._qa_pipeline = pipeline(
                "document-question-answering",
                model=Config.AI_MODEL_QA,
                device=device,
            )
            logger.info("QA model loaded")
        return self._qa_pipeline

    def translate(self, text, src_lang, tgt_lang):
        """Translate text from source to target language"""
        try:
            result = self.translate_pipe(text, src_lang=src_lang, tgt_lang=tgt_lang)
            return result[0]['translation_text']
        except Exception as e:
            logger.error(f"Translation error: {e}")
            return text  # Return original text on error

    def answer_question(self, question, context):
        """Answer question based on context"""
        try:
            result = self.qa_pipeline(question=question, context=context)
            if isinstance(result, dict):
                return result['answer']
            elif isinstance(result, list) and len(result) > 0:
                return result[0]['answer']
            else:
                return "I'm having trouble processing that question."
        except Exception as e:
            logger.error(f"QA error: {e}")
            return "I'm having trouble processing that question."
    
    def answer_question_with_context(self, question, base_context, conversation_history=None):
        """
        Enhanced answer function that includes conversation history and response diversity.

        Args:
            question: The question to answer
            base_context: The base context for answering
            conversation_history: List of (question, answer) tuples for context

        Returns:
            str: The generated answer
        """
        enhanced_context = self._build_enhanced_context(base_context, conversation_history)

        question_hash = self._get_question_hash(question)
        if self._is_repetitive_question(question, conversation_history):
            return self._generate_diverse_response(question, enhanced_context, question_hash)

        try:
            result = self.qa_pipeline(
                question=question,
                context=enhanced_context,
                top_k=Config.AI_QA_TOP_K_PRIMARY,
                max_answer_len=Config.AI_MAX_ANSWER_LEN,
            )

            response = self._select_diverse_response(result, question, question_hash)
            return response
        except Exception as e:
            logger.error(f"Error in QA pipeline: {e}")
            return "I apologize, but I'm having trouble processing your question right now."
    
    def _build_enhanced_context(self, base_context, conversation_history):
        """
        Build enhanced context by combining base context with recent conversation.

        Args:
            base_context: The base context string
            conversation_history: List of (question, answer) tuples

        Returns:
            str: Enhanced context with conversation history
        """
        if not conversation_history:
            return base_context

        recent_exchanges = conversation_history[-Config.CONVERSATION_RECENT_EXCHANGES:]
        conversation_context = "\n".join([
            f"Previous Question: {q}\nPrevious Answer: {a}\n"
            for q, a in recent_exchanges
        ])

        return f"{base_context}\n\nRecent Conversation Context:\n{conversation_context}"
    
    def _get_question_hash(self, question):
        """
        Create a hash for the question to detect similar questions.

        Args:
            question: The question string to hash

        Returns:
            str: MD5 hash of the normalized question
        """
        normalized = question.lower().strip()
        return hashlib.md5(normalized.encode()).hexdigest()
    
    def _is_repetitive_question(self, question, conversation_history):
        """
        Check if the current question is repetitive based on recent conversation.

        Args:
            question: The current question
            conversation_history: List of (question, answer) tuples

        Returns:
            bool: True if question is repetitive, False otherwise
        """
        if not conversation_history:
            return False

        recent_questions = [q for q, a in conversation_history[-Config.REPETITION_HISTORY_WINDOW:]]

        question_lower = question.lower()
        for recent_q in recent_questions:
            if self._calculate_similarity(question_lower, recent_q.lower()) > Config.REPETITION_SIMILARITY_THRESHOLD:
                return True

        return False
    
    def _calculate_similarity(self, text1, text2):
        """
        Calculate Jaccard similarity between two texts based on common words.

        Args:
            text1: First text string
            text2: Second text string

        Returns:
            float: Similarity score between 0 and 1
        """
        words1 = set(text1.split())
        words2 = set(text2.split())

        if not words1 or not words2:
            return 0.0

        intersection = words1.intersection(words2)
        union = words1.union(words2)

        return len(intersection) / len(union)
    
    def _generate_diverse_response(self, question, context, question_hash):
        """
        Generate a diverse response for repetitive questions.

        Args:
            question: The question string
            context: The context for answering
            question_hash: MD5 hash of the question

        Returns:
            str: A diverse response different from previous ones
        """
        if question_hash in self.response_cache:
            cached_responses = self.response_cache[question_hash]
            if len(cached_responses) > 1:
                return random.choice([r for r in cached_responses if r != cached_responses[-1]])

        try:
            result = self.qa_pipeline(
                question=question,
                context=context,
                top_k=Config.AI_QA_TOP_K_DIVERSE,
                max_answer_len=Config.AI_MAX_ANSWER_LEN,
            )

            if isinstance(result, list):
                slice_end = min(len(result), 3)
                response = random.choice(result[:slice_end])['answer']
            else:
                result_list = list(result) if hasattr(result, '__iter__') and not isinstance(result, dict) else result
                if isinstance(result_list, list) and len(result_list) > 0:
                    response = result_list[0]['answer']
                elif isinstance(result_list, dict):
                    response = result_list['answer']
                else:
                    response = "I understand you're asking about this topic."

            if question_hash not in self.response_cache:
                self.response_cache[question_hash] = []
            self.response_cache[question_hash].append(response)
            return response
        except Exception as e:
            logger.error(f"Error generating diverse response: {e}")
            return "I understand you're asking about this topic."
    
    def _select_diverse_response(self, result, question, question_hash):
        """
        Select the most appropriate response from multiple candidates.

        Args:
            result: QA pipeline result (dict or list)
            question: The question string
            question_hash: MD5 hash of the question

        Returns:
            str: The selected response
        """
        if isinstance(result, list):
            candidates = [r['answer'] for r in result]
            unique_candidates = list(dict.fromkeys(candidates))

            if len(unique_candidates) > 1:
                response = random.choice(unique_candidates[:2])
            else:
                response = unique_candidates[0]
        else:
            result_list = list(result) if hasattr(result, '__iter__') and not isinstance(result, dict) else result
            if isinstance(result_list, list) and len(result_list) > 0:
                response = result_list[0]['answer']
            elif isinstance(result_list, dict):
                response = result_list['answer']
            else:
                response = "I'm having trouble processing that question."

        if question_hash not in self.response_cache:
            self.response_cache[question_hash] = []
        self.response_cache[question_hash].append(response)

        return response

ai_service = AIService()

def translate(text, src_lang, tgt_lang):
    return ai_service.translate(text, src_lang, tgt_lang)

def answer_question(question, context):
    return ai_service.answer_question(question, context)

def answer_question_with_context(question, context, conversation_history=None):
    return ai_service.answer_question_with_context(question, context, conversation_history)
