
from transformers.utils.logging import set_verbosity_error

set_verbosity_error() # disabling logs


import torch
print(torch.cuda.is_available())
print(torch.cuda.get_device_name(0)) # print GPU



from transformers import pipeline


languages = {
	'ace_Arab' : 'Acehnese',
	'ace_Latn' : 'Acehnese', 
	'acm_Arab' : 'Mesopotamian Arabic', 
	'acq_Arab' : 'Ta izzi-Adeni Arabic', 
	'aeb_Arab' : 'Tunisian Arabic', 
	'afr_Latn' : 'Afrikaans', 
	'ajp_Arab' : 'South Levantine Arabic', 
	'aka_Latn' : 'Akan', 
	'amh_Ethi' : 'Amharic', 
	'apc_Arab' : 'North Levantine Arabic', 
	'arb_Arab' : 'Standard Arabic', 
	'ars_Arab' : 'Najdi Arabic', 
	'ary_Arab' : 'Moroccan Arabic', 
	'arz_Arab' : 'Egyptian Arabic', 
	'asm_Beng' : 'Assamese', 
	'ast_Latn' : 'Asturian', 
	'awa_Deva' : 'Awadhi', 
	'ayr_Latn' : 'Aymara', 
	'azb_Arab' : 'South Azerbaijani', 
	'azj_Latn' : 'North Azerbaijani', 
	'bak_Cyrl' : 'Bashkir', 
	'bam_Latn' : 'Bambara', 
	'ban_Latn' : 'Balinese', 
	'bel_Cyrl' : 'Belarusian', 
	'bem_Latn' : 'Bemba', 
	'ben_Beng' : 'Bengali',
	'bho_Deva' : 'Bhojpuri', 
	'bjn_Arab' : 'Banjar', 
	'bjn_Latn' : 'Banjar', 
	'bod_Tibt' : 'Tibetan', 
	'bos_Latn' : 'Bosnian', 
	'bug_Latn' : 'Buginese', 
	'bul_Cyrl' : 'Bulgarian', 
	'cat_Latn' : 'Catalan', 
	'ceb_Latn' : 'Cebuano', 
	'ces_Latn' : 'Czech', 
	'cjk_Latn' : 'Chokwe', 
	'ckb_Arab' : 'Central Kurdish (Sorani)', 
	'crh_Latn' : 'Crimean Tatar', 
	'cym_Latn' : 'Welsh', 
	'dan_Latn' : 'Danish', 
	'deu_Latn' : 'German', 
	'dik_Latn' : 'Southwestern Dinka', 
	'dyu_Latn' : 'Dyula', 
	'dzo_Tibt' : 'Dzongkha', 
	'ell_Grek' : 'Greek', 
	'eng_Latn' : 'English', 
	'epo_Latn' : 'Esperanto', 
	'est_Latn' : 'Estonian', 
	'eus_Latn' : 'Basque', 
	'ewe_Latn' : 'Ewe', 
	'fao_Latn' : 'Faroese', 
	'pes_Arab' : 'Iranian Persian', 
	'fij_Latn' : 'Fijian', 
	'fin_Latn' : 'Finnish', 
	'fon_Latn' : 'Fon', 
	'fra_Latn' : 'French', 
	'fur_Latn' : 'Friulian', 
	'fuv_Latn' : 'Nigerian Fulfulde', 
	'gla_Latn' : 'Scottish Gaelic', 
	'gle_Latn' : 'Irish', 
	'glg_Latn' : 'Galician', 
	'grn_Latn' : 'Guarani', 
	'guj_Gujr' : 'Gujarati', 
	'hat_Latn' : 'Haitian Creole', 
	'hau_Latn' : 'Hausa', 
	'heb_Hebr' : 'Hebrew', 
	'hin_Deva' : 'Hindi', 
	'hne_Deva' : 'Chhattisgarhi', 
	'hrv_Latn' : 'Croatian', 
	'hun_Latn' : 'Hungarian', 
	'hye_Armn' : 'Armenian', 
	'ibo_Latn' : 'Igbo', 
	'ilo_Latn' : 'Ilocano', 
	'ind_Latn' : 'Indonesian', 
	'isl_Latn' : 'Icelandic', 
	'ita_Latn' : 'Italian', 
	'jav_Latn' : 'Javanese', 
	'jpn_Jpan' : 'Japanese', 
	'kab_Latn' : 'Kabyle', 
	'kac_Latn' : 'Jingpho (Kachin)', 
	'kam_Latn' : 'Kamba', 
	'kan_Knda' : 'Kannada', 
	'kas_Arab' : 'Kashmiri', 
	'kas_Deva' : 'Kashmiri', 
	'kat_Geor' : 'Georgian', 
	'knc_Arab' : 'Kanuri', 
	'knc_Latn' : 'Kanuri', 
	'kaz_Cyrl' : 'Kazakh', 
	'kbp_Latn' : 'Kabiyè', 
	'kea_Latn' : 'Kabuverdianu (Cape Verde Creole)',
	'khm_Khmr' : 'Khmer', 
	'kik_Latn' : 'Kikuyu', 
	'kin_Latn' : 'Kinyarwanda', 
	'kir_Cyrl' : 'Kyrgyz', 
	'kmb_Latn' : 'Kimbundu', 
	'kon_Latn' : 'Kongo', 
	'kor_Hang' : 'Korean', 
	'kmr_Latn' : 'Northern Kurdish (Kurmanji)', 
	'lao_Laoo' : 'Lao', 
	'lvs_Latn' : 'Standard Latvian', 
	'lij_Latn' : 'Ligurian', 
	'lim_Latn' : 'Limburgish', 
	'lin_Latn' : 'Lingala',
	'lit_Latn' : 'Lithuanian', 
	'lmo_Latn' : 'Lombard', 
	'ltg_Latn' : 'Latgalian', 
	'ltz_Latn' : 'Luxembourgish', 
	'lua_Latn' : 'Luba-Lulua', 
	'lug_Latn' : 'Ganda', 
	'luo_Latn' : 'Luo', 
	'lus_Latn' : 'Mizo (Lushai)', 
	'mag_Deva' : 'Magahi', 
	'mai_Deva' : 'Maithili', 
	'mal_Mlym' : 'Malayalam', 
	'mar_Deva' : 'Marathi', 
	'min_Latn' : 'Minangkabau', 
	'mkd_Cyrl' : 'Macedonian', 
	'plt_Latn' : 'Plateau Malagasy', 
	'mlt_Latn' : 'Maltese', 
	'mni_Beng' : 'Manipuri (Meitei)', 
	'khk_Cyrl' : 'Halh Mongolian', 
	'mos_Latn' : 'Mooré', 
	'mri_Latn' : 'Māori', 
	'zsm_Latn' : 'Malay (generic)', 
	'mya_Mymr' : 'Burmese', 
	'nld_Latn' : 'Dutch', 
	'nno_Latn' : 'Norwegian Nynorsk', 
	'nob_Latn' : 'Norwegian Bokmål', 
	'npi_Deva' : 'Nepali (India)', 
	'nso_Latn' : 'Northern Sotho', 
	'nus_Latn' : 'Nuer', 
	'nya_Latn' : 'Nyanja (Chichewa)', 
	'oci_Latn' : 'Occitan', 
	'gaz_Latn' : 'West Central Oromo', 
	'ory_Orya' : 'Odia', 
	'pag_Latn' : 'Pangasinan', 
	'pan_Guru' : 'Punjabi', 
	'pap_Latn' : 'Papiamento', 
	'pol_Latn' : 'Polish', 
	'por_Latn' : 'Portuguese', 
	'prs_Arab' : 'Dari Persian', 
	'pbt_Arab' : 'Southern Pashto', 
	'quy_Latn' : 'Ayacucho Quechua', 
	'ron_Latn' : 'Romanian', 
	'run_Latn' : 'Rundi (Kirundi)', 
	'rus_Cyrl' : 'Russian', 
	'sag_Latn' : 'Sango', 
	'san_Deva' : 'Sanskrit', 
	'sat_Beng' : 'Santali', 
	'scn_Latn' : 'Sicilian', 
	'shn_Mymr' : 'Shan', 
	'sin_Sinh' : 'Sinhala', 
	'slk_Latn' : 'Slovak',
	'slv_Latn' : 'Slovenian', 
	'smo_Latn' : 'Samoan', 
	'sna_Latn' : 'Shona', 
	'snd_Arab' : 'Sindhi', 
	'som_Latn' : 'Somali', 
	'sot_Latn' : 'Southern Sotho', 
	'spa_Latn' : 'Spanish', 
	'als_Latn' : 'Tosk Albanian', 
	'srd_Latn' : 'Sardinian', 
	'srp_Cyrl' : 'Serbian', 
	'ssw_Latn' : 'Swati',
	'sun_Latn' : 'Sundanese', 
	'swe_Latn' : 'Swedish', 
	'swh_Latn' : 'Swahili', 
	'szl_Latn' : 'Silesian', 
	'tam_Taml' : 'Tamil', 
	'tat_Cyrl' : 'Tatar', 
	'tel_Telu' : 'Telugu', 
	'tgk_Cyrl' : 'Tajik', 
	'tgl_Latn' : 'Tagalog', 
	'tha_Thai' : 'Thai', 
	'tir_Ethi' : 'Tigrinya', 
	'taq_Latn' : 'Tamasheq (Latin)', 
	'taq_Tfng' : 'Tamasheq (Tifinagh)', 
	'tpi_Latn' : 'Tok Pisin', 
	'tsn_Latn' : 'Tswana', 
	'tso_Latn' : 'Tsonga', 
	'tuk_Latn' : 'Turkmen', 
	'tum_Latn' : 'Tumbuka', 
	'tur_Latn' : 'Turkish', 
	'twi_Latn' : 'Twi', 
	'tzm_Tfng' : 'Central Atlas Tamazight', 
	'uig_Arab' : 'Uyghur', 
	'ukr_Cyrl' : 'Ukrainian', 
	'umb_Latn' : 'Umbundu', 
	'urd_Arab' : 'Urdu', 
	'uzn_Latn' : 'Northern Uzbek', 
	'vec_Latn' : 'Venetian', 
	'vie_Latn' : 'Vietnamese', 
	'war_Latn' : 'Waray', 
	'wol_Latn' : 'Wolof', 
	'xho_Latn' : 'Xhosa', 
	'ydd_Hebr' : 'Eastern Yiddish', 
	'yor_Latn' : 'Yoruba', 
	'yue_Hant' : 'Cantonese',
	'zho_Hans' : 'Mandarin Chinese', 
	'zho_Hant' : 'Mandarin Chinese',
	'zul_Latn' : 'Zulu' 
}
languages = dict(sorted(languages.items(), key=lambda item: item[1]))


translate_pipe = pipeline("translation", model="facebook/nllb-200-distilled-600M") #200+ languages



qa_pipeline = pipeline("question-answering", model="deepset/roberta-base-squad2",
                       device=0)


#ans_improve_pipe = pipeline("text-generation", model="deepseek-ai/DeepSeek-R1-Distill-Qwen-1.5B",
#                device=0,
#                max_new_tokens=32768,
#)



with open('llm-script.txt', 'r', encoding='utf-8') as f:
    read_content = f.read()




from flask import Flask, render_template, request, jsonify, session

app = Flask(__name__)

app.secret_key = 'bananaaaaaa'  #Replace with a secure key, use rng


messages = [] # Stores a list of (input, response) pairs for commmunication with frontend


@app.route("/", methods=["GET", "POST"])
def index():
    global messages #unsecure? ; all users can see? ; maybe replace with sessions?
     
    selected_language = session.get("selected_language", "eng_Latn")


    if request.method == "POST":

        #if dropdown menu was used
        if 'language' in request.form:
            #gets value from form
            selected_language = request.form.get("language")
            #store value
            session["selected_language"] = selected_language
            return jsonify({
                "selected_language": languages.get(selected_language, "Unknown")
            })

        #if qn-ans form was used
        elif 'user_input' in request.form:

            user_input = request.form.get("user_input")
            
            if selected_language != "eng_Latn":
                
                src_eng_translation = translate_pipe(user_input, src_lang=selected_language, tgt_lang="eng_Latn")
                eng_lang = src_eng_translation[-1]["translation_text"]      

                #pass the qn
                qa_result = qa_pipeline(question=eng_lang, context=read_content)
                qa_result_ans = qa_result["answer"]
                print(f"qa_response: {qa_result_ans}")
                

                #deepseek_prompt = [] by default each form submission
                #deepseek_prompt = [
                #    {"role": "assistant", "content": "You are an expert in English literature and sentence rephrasing. The user will provide a question and a simplified answer. Your task is to rephrase only the answer into a single, clear, grammatically correct declarative sentence using the appropriate tense that matches the question and the sentence exactly preserves the original wording and content without any changes or reinterpretations. Strict Rule: Do not fact-check, correct, or alter the user's answer. Do not turn the answer into a question or add any new information. Do not use any contraction. Do not use questions in the answer. Simply embed the original answer exactly as given into a statement. Do not change any names, places, phrases, or terms from the question or answer. Use the exact words and spelling as provided. Example:Q: Who killed Gandhi?A: john abhraham Response: John Abhraham killed Gandhi.Q: When was Tagore born?A: 12/2/1201 Response: Tagore was born on 12/2/1201.Q: who was bhagat singh?A: An Indian anti-colonial revolutionary. Response: Bhagat Singh an Indian anti-colonial revolutionary."},
                #    {"role": "user", "content": f"Question: {user_input}?  Answer: {qa_result_ans} "},
                #]
                #unformatted_response = ans_improve_pipe(deepseek_prompt)[-1]["generated_text"][-1]["content"]
                #deepseek_result_ans = unformatted_response.split("</think>")[-1].strip()
                #print(f"response: {deepseek_result_ans}")
                deepseek_result_ans = qa_result_ans #dlt this line if deepseek is enabled

                eng_src_translation = translate_pipe(deepseek_result_ans, src_lang="eng_Latn", tgt_lang=selected_language)
                response = eng_src_translation[-1]["translation_text"]
                
                
            elif selected_language == "eng_Latn":  #else
          
                #pass the qn
                qa_result = qa_pipeline(question=user_input, context=read_content)
                qa_response = qa_result["answer"]
                print(f"qa_response: {qa_response}")

                
                #deepseek_prompt = [] by default each form submission
                #deepseek_prompt = [
                #    {"role": "assistant", "content": "You are an expert in English literature and sentence rephrasing. The user will provide a question and a simplified answer. Your task is to rephrase only the answer into a single, clear, grammatically correct declarative sentence using the appropriate tense that matches the question and the sentence exactly preserves the original wording and content without any changes or reinterpretations. Strict Rule: Do not fact-check, correct, or alter the user's answer. Do not turn the answer into a question or add any new information. Do not use any contraction. Do not use questions in the answer. Simply embed the original answer exactly as given into a statement. Do not change any names, places, phrases, or terms from the question or answer. Use the exact words and spelling as provided. Example:Q: Who killed Gandhi?A: john abhraham Response: John Abhraham killed Gandhi.Q: When was Tagore born?A: 12/2/1201 Response: Tagore was born on 12/2/1201.Q: who was bhagat singh?A: An Indian anti-colonial revolutionary. Response: Bhagat Singh an Indian anti-colonial revolutionary."},
                #    {"role": "user", "content": f"Question: {user_input}?  Answer: {qa_response} "},
                #]
                #unformatted_response = ans_improve_pipe(deepseek_prompt)[-1]["generated_text"][-1]["content"]
                #response = unformatted_response.split("</think>")[-1].strip()
                #print(f"response: {response}")
                response = qa_response #dlt this line if deepseek is enabled

            messages.append((user_input, response))
            return jsonify({'user_input': user_input, 'answer': response})

    return render_template("index.html", languages=languages, messages=messages,
                           selected_language=selected_language)#helps in setting
                            #default value to english


@app.route("/reset")
def reset():
    session.clear()
    return "Session cleared!"




if __name__ == "__main__":
    app.run(debug=False)









    
