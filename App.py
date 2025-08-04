from flask import Flask
from text_api import text_api
from link_api import link_api

app = Flask(__name__)


app.register_blueprint(text_api)
app.register_blueprint(link_api)

if __name__ == '__main__':
    app.run(debug=True, port=5001)

