from flask import Flask
from flask_cors import CORS
import logging
import os
from app.routes.document_routes import upload_bp
from app.routes.main_routes import main_bp
from app.routes.links_routes import links_bp
from app.routes.api_routes import api_bp
from app.config import Config
from app.routes.whisper_links import whisper_links


def setup_logging(app):
    """Configure application logging"""
    log_level = getattr(logging, app.config['LOG_LEVEL'].upper(), logging.INFO)

    # Create formatters
    formatter = logging.Formatter(
        '[%(asctime)s] %(levelname)s in %(module)s: %(message)s'
    )

    # Console handler
    console_handler = logging.StreamHandler()
    console_handler.setLevel(log_level)
    console_handler.setFormatter(formatter)

    # File handler
    try:
        file_handler = logging.FileHandler(app.config['LOG_FILE'])
        file_handler.setLevel(log_level)
        file_handler.setFormatter(formatter)
        app.logger.addHandler(file_handler)
    except Exception as e:
        app.logger.warning(f"Could not create log file: {e}")

    # Add handlers to app logger
    app.logger.addHandler(console_handler)
    app.logger.setLevel(log_level)

    # Set logging for other modules
    logging.basicConfig(level=log_level, handlers=[console_handler])

def create_app():
    """Create and configure Flask application"""
    app = Flask(__name__)
    app.config.from_object(Config)

    # Initialize config (create directories, etc.)
    Config.init_app(app)

    # Setup logging
    setup_logging(app)
    app.logger.info("Flask application starting...")

    # Enable CORS for mobile app
    CORS(app, resources={
        r"/api/*": {
            "origins": "*",  # In production, specify your app's domains
            "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
            "allow_headers": ["Content-Type", "Authorization"],
            "supports_credentials": True
        }
    })

    # Register blueprints
    app.register_blueprint(upload_bp, url_prefix="/upload")
    app.register_blueprint(main_bp)
    app.register_blueprint(links_bp, url_prefix="/links")
    app.register_blueprint(api_bp, url_prefix="/api/v1")
    app.register_blueprint(whisper_links)
    

    app.logger.info(f"Registered blueprints: {list(app.blueprints.keys())}")

    return app