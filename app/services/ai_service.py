from transformers import pipeline
import torch
from transformers.utils.logging import set_verbosity_error
set_verbosity_error()

translate_pipe = pipeline("translation", model="facebook/nllb-200-distilled-600M")
qa_pipeline = pipeline("question-answering", model="deepset/roberta-base-squad2",
                       device=0 if torch.cuda.is_available() else -1)

def translate(text, src_lang, tgt_lang):
    return translate_pipe(text, src_lang=src_lang, tgt_lang=tgt_lang)[0]['translation_text']

def answer_question(question, context):
    return qa_pipeline(question=question, context=context)['answer']
