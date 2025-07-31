from flask import Blueprint, request, render_template, session, jsonify
from app.services.context_loader import load_context
from app.services.ai_service import translate, answer_question
from app.constants.languages import languages

main_bp = Blueprint("main", __name__)
context = load_context()
messages = []

@main_bp.route("/hello")
def hello():
    return "Hello from main route!"

@main_bp.route("/", methods=["GET", "POST"])
def index():
    print("Main route accessed")
    selected_language = session.get("selected_language", "eng_Latn")

    if request.method == "POST":
        if "language" in request.form:
            selected_language = request.form["language"]
            session["selected_language"] = selected_language
            return jsonify({"selected_language": languages.get(selected_language, "Unknown")})

        elif "user_input" in request.form:
            user_input = request.form["user_input"]
            if selected_language != "eng_Latn":
                eng_question = translate(user_input, src_lang=selected_language, tgt_lang="eng_Latn")
                eng_answer = answer_question(eng_question, context)
                translated_answer = translate(eng_answer, src_lang="eng_Latn", tgt_lang=selected_language)
                response = translated_answer
            else:
                response = answer_question(user_input, context)

            messages.append((user_input, response))
            return jsonify({"user_input": user_input, "answer": response})

    return render_template("index.html", messages=messages,
                           selected_language=selected_language,
                           languages=languages)

@main_bp.route("/reset")
def reset():
    session.clear()
    return "Session cleared!"
