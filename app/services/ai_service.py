from transformers import AutoModelForQuestionAnswering, AutoTokenizer
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
        self._qa_tokenizer = None
        self._qa_model = None
        self.response_cache = LRUCache(maxsize=cache_size)
        self.conversation_patterns = {}
        logger.info("AI Service initialized (models will load on first use)")

    @property
    def translate_pipe(self):
        """Lazy load translation pipeline"""
        if self._translate_pipe is None:
            from transformers import pipeline
            logger.info("Loading translation model...")
            self._translate_pipe = pipeline(
                "translation",
                model=Config.AI_MODEL_TRANSLATION,
            )
            logger.info("Translation model loaded")
        return self._translate_pipe

    def _load_qa_model(self):
        """Lazy load QA model and tokenizer"""
        if self._qa_model is None:
            logger.info("Loading QA model...")
            self._qa_tokenizer = AutoTokenizer.from_pretrained(Config.AI_MODEL_QA)
            self._qa_model = AutoModelForQuestionAnswering.from_pretrained(Config.AI_MODEL_QA)
            logger.info("QA model loaded")

    def _run_qa(self, question, context):
        """Run QA directly using model and tokenizer"""
        self._load_qa_model()
        try:
            inputs = self._qa_tokenizer(
                question,
                context,
                return_tensors="pt",
                truncation=True,
                max_length=512
            )
            if torch is not None:
                with torch.no_grad():
                    outputs = self._qa_model(**inputs)
            else:
                outputs = self._qa_model(**inputs)

            start = outputs.start_logits.argmax()
            end = outputs.end_logits.argmax() + 1
            answer = self._qa_tokenizer.convert_tokens_to_string(
                self._qa_tokenizer.convert_ids_to_tokens(
                    inputs["input_ids"][0][start:end]
                )
            )
            return answer.strip() or "I'm having trouble finding an answer to that."
        except Exception as e:
            logger.error(f"QA error: {e}")
            return "I'm having trouble processing that question."

    def translate(self, text, src_lang, tgt_lang):
        """Translate text from source to target language"""
        try:
            result = self.translate_pipe(text, src_lang=src_lang, tgt_lang=tgt_lang)
            return result[0]['translation_text']
        except Exception as e:
            logger.error(f"Translation error: {e}")
            return text

    def answer_question(self, question, context):
        """Answer question based on context"""
        return self._run_qa(question, context)

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
            response = self._run_qa(question, enhanced_context)

            if question_hash not in self.response_cache:
                self.response_cache[question_hash] = []
            self.response_cache[question_hash].append(response)

            return response
        except Exception as e:
            logger.error(f"Error in QA pipeline: {e}")
            return "I apologize, but I'm having trouble processing your question right now."

    def _build_enhanced_context(self, base_context, conversation_history):
        """Build enhanced context by combining base context with recent conversation."""
        if not conversation_history:
            return base_context

        recent_exchanges = conversation_history[-Config.CONVERSATION_RECENT_EXCHANGES:]
        conversation_context = "\n".join([
            f"Previous Question: {q}\nPrevious Answer: {a}\n"
            for q, a in recent_exchanges
        ])

        return f"{base_context}\n\nRecent Conversation Context:\n{conversation_context}"

    def _get_question_hash(self, question):
        """Create a hash for the question to detect similar questions."""
        normalized = question.lower().strip()
        return hashlib.md5(normalized.encode()).hexdigest()

    def _is_repetitive_question(self, question, conversation_history):
        """Check if the current question is repetitive based on recent conversation."""
        if not conversation_history:
            return False

        recent_questions = [q for q, a in conversation_history[-Config.REPETITION_HISTORY_WINDOW:]]
        question_lower = question.lower()
        for recent_q in recent_questions:
            if self._calculate_similarity(question_lower, recent_q.lower()) > Config.REPETITION_SIMILARITY_THRESHOLD:
                return True

        return False

    def _calculate_similarity(self, text1, text2):
        """Calculate Jaccard similarity between two texts based on common words."""
        words1 = set(text1.split())
        words2 = set(text2.split())

        if not words1 or not words2:
            return 0.0

        intersection = words1.intersection(words2)
        union = words1.union(words2)

        return len(intersection) / len(union)

    def _generate_diverse_response(self, question, context, question_hash):
        """Generate a diverse response for repetitive questions."""
        if question_hash in self.response_cache:
            cached_responses = self.response_cache[question_hash]
            if len(cached_responses) > 1:
                return random.choice([r for r in cached_responses if r != cached_responses[-1]])

        try:
            response = self._run_qa(question, context)

            if question_hash not in self.response_cache:
                self.response_cache[question_hash] = []
            self.response_cache[question_hash].append(response)
            return response
        except Exception as e:
            logger.error(f"Error generating diverse response: {e}")
            return "I understand you're asking about this topic."


ai_service = AIService()


def translate(text, src_lang, tgt_lang):
    return ai_service.translate(text, src_lang, tgt_lang)


def answer_question(question, context):
    return ai_service.answer_question(question, context)


def answer_question_with_context(question, context, conversation_history=None):
    return ai_service.answer_question_with_context(question, context, conversation_history)