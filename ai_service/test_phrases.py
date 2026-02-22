from __future__ import annotations

import json
import urllib.error
import urllib.parse
import urllib.request

PERSPECTIVE_API_KEY = ""
PY_AI_URL = "http://127.0.0.1:8008"

SAMPLES = [
    "I like this post",
    "Thanks for sharing this interview tip",
    "I hate you",
    "Go die",
    "This is unrelated gaming chat",
]


def call_perspective(text: str, api_key: str) -> float:
    endpoint = (
        "https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze?key="
        + urllib.parse.quote(api_key)
    )
    payload = {
        "comment": {"text": text},
        "languages": ["en"],
        "requestedAttributes": {"TOXICITY": {}},
    }
    req = urllib.request.Request(
        endpoint,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=8) as resp:
        parsed = json.loads(resp.read().decode("utf-8"))
        return float(parsed["attributeScores"]["TOXICITY"]["summaryScore"]["value"])


def call_embed(text: str) -> tuple[int, str]:
    endpoint = PY_AI_URL.rstrip("/") + "/embed"
    payload = {"text": text, "type": "comment"}
    req = urllib.request.Request(
        endpoint,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=8) as resp:
        parsed = json.loads(resp.read().decode("utf-8"))
        return int(parsed.get("dim", 0)), str(parsed.get("model", "unknown"))


def main() -> None:
    print("=== AI Test Phrases ===")
    for idx, text in enumerate(SAMPLES, start=1):
        print(f"{idx:02d}. {text}")
        try:
            dim, model = call_embed(text)
            print(f"   embed: model={model}, dim={dim}")
        except urllib.error.URLError as ex:
            print(f"   embed error: {ex}")

        if PERSPECTIVE_API_KEY.strip():
            try:
                tox = call_perspective(text, PERSPECTIVE_API_KEY.strip())
                print(f"   perspective toxicity={tox:.4f}")
            except Exception as ex:
                print(f"   perspective error: {ex}")
        else:
            print("   perspective skipped (set PERSPECTIVE_API_KEY in this file)")


if __name__ == "__main__":
    main()
