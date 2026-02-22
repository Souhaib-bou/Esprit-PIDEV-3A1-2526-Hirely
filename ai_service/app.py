from __future__ import annotations

from functools import lru_cache
from typing import Literal

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer


MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"
app = FastAPI(title="Hirely AI Service", version="1.0.0")


class EmbedRequest(BaseModel):
    text: str
    type: Literal["post", "comment"] = "post"


class EmbedResponse(BaseModel):
    embedding: list[float]
    dim: int
    model: str


@lru_cache(maxsize=1)
def get_model() -> SentenceTransformer:
    return SentenceTransformer(MODEL_NAME)


@app.get("/health")
def health() -> dict:
    return {"ok": True, "model": MODEL_NAME}


@app.post("/embed", response_model=EmbedResponse)
def embed(req: EmbedRequest) -> EmbedResponse:
    text = (req.text or "").strip()
    if not text:
        raise HTTPException(status_code=400, detail="text is required")

    model = get_model()
    vector = model.encode(text, normalize_embeddings=True).tolist()
    return EmbedResponse(embedding=[float(v) for v in vector], dim=len(vector), model=MODEL_NAME)
