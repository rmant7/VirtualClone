#from flask import Flask
from app import create_app
#import os

app = create_app()
# app = Flask(__name__)


# @app.route("/")
# def hello():
#     return "Flask is alive!"


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5050, debug=True)
