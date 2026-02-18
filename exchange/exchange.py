import os
import time
import math
from flask import Flask, jsonify

app = Flask(__name__)

# Dummy BTC price feed: smooth oscillation over time.
START = time.time()
BASE_PRICE = float(os.getenv("BASE_PRICE", "50000.0"))
AMPLITUDE = float(os.getenv("AMPLITUDE", "250.0"))
PERIOD_SEC = float(os.getenv("PERIOD_SEC", "30.0"))

@app.get("/health")
def health():
    return jsonify(status="ok")

@app.get("/price")
def price():
    t = time.time() - START
    p = BASE_PRICE + AMPLITUDE * math.sin(2.0 * math.pi * (t / PERIOD_SEC))
    return jsonify(symbol="BTC-USD", price=round(p, 2), ts=int(time.time()))

if __name__ == "__main__":
    port = int(os.getenv("PORT", "8000"))
    app.run(host="0.0.0.0", port=port)
