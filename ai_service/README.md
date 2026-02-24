# AI Service

Start local AI service:

```bash
pip install -r requirements.txt
uvicorn app:app --host 127.0.0.1 --port 8008
```

Available endpoints:
- `POST /score` -> relevance/category/quality/duplicateSimilarity and reasons used by Java moderation policy
- `GET /health` -> service health + active model

Java app environment variables:

- `PERSPECTIVE_API_KEY=<your_key>`
- `PY_AI_URL=http://127.0.0.1:8008`
