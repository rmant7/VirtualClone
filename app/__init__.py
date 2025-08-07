from flask import Flask
from app.routes.document_routes import upload_bp
from app.routes.main_routes import main_bp
from app.routes.links_routes import links_bp
from app.config import Config

def create_app():
    print("Creating Flask app")
    app = Flask(__name__)
    # base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    # template_dir = os.path.join(base_dir, "templates")
    #app = Flask(__name__, template_folder=template_dir)
    app.config.from_object(Config)
    app.secret_key = "bananaaaaaa"

    app.register_blueprint(upload_bp, url_prefix="/upload")
    app.register_blueprint(main_bp)
    app.register_blueprint(links_bp, url_prefix="/links")

    return app