import torch
from datasets import load_dataset
from transformers import AutoTokenizer, AutoModelForCausalLM, TrainingArguments, Trainer, DataCollatorForLanguageModeling
from peft import LoraConfig, get_peft_model, prepare_model_for_kbit_training
import os

class AITrainingService:
    def __init__(self, model_id="openai-community/gpt2", data_file="data/train.jsonl", output_dir="output", max_length=256):
        self.model_id = model_id
        self.data_file = data_file
        self.output_dir = output_dir
        self.max_length = max_length
        self.tokenizer = None
        self.model = None
        self.dataset = None
        self.tokenized = None
        self.trainer = None

    def load_ds(self):
        self.dataset = load_dataset("json", data_files=self.data_file)["train"]

    def load_tokenizer_and_model(self):
        self.tokenizer = AutoTokenizer.from_pretrained(self.model_id)
        self.tokenizer.pad_token = self.tokenizer.eos_token
        self.model = AutoModelForCausalLM.from_pretrained(self.model_id)

    def enable_lora(self):
        peft_config = LoraConfig(
            r=8,
            lora_alpha=32,
            target_modules=["c_attn", "q_attn", "v_attn"] if "gpt" in self.model_id else None,
            lora_dropout=0.05,
            bias="none",
            task_type="CAUSAL_LM",
        )
        self.model = get_peft_model(self.model, peft_config)

    def tokenize(self):
        def tokenize_fn(batch):
            return self.tokenizer(batch["text"], truncation=True, padding="max_length", max_length=self.max_length)
        self.tokenized = self.dataset.map(tokenize_fn, batched=True, remove_columns=["text"])

    def setup_trainer(self):
        args = TrainingArguments(
            output_dir=self.output_dir,
            per_device_train_batch_size=4,
            logging_steps=10,
            num_train_epochs=3,
            save_strategy="epoch",
            save_total_limit=1,
            fp16=torch.cuda.is_available(),
            report_to="none"
        )
        self.trainer = Trainer(
            model=self.model,
            args=args,
            train_dataset=self.tokenized,
            data_collator=DataCollatorForLanguageModeling(self.tokenizer, mlm=False)
        )

    def train(self):
        self.trainer.train()

    def save_model(self):
        base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "../.."))
        data_dir = os.path.join(base_dir, "data/output")
        os.makedirs(data_dir, exist_ok=True)
        self.trainer.save_model(os.path.join(data_dir, "lora-finetuned"))

    def run(self):
        print("🔄 Loading dataset...")
        self.load_ds()

        print("📦 Loading tokenizer and model...")
        self.load_tokenizer_and_model()

        print("✨ Applying LoRA...")
        self.enable_lora()

        print("✂️ Tokenizing data...")
        self.tokenize()

        print("⚙️ Setting up trainer...")
        self.setup_trainer()

        print("🚀 Training...")
        self.train()

        print("💾 Saving model...")
        self.save_model()

        print("✅ Training complete.")
