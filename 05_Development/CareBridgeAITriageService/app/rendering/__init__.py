"""Deterministic response rendering. No LLM writes user-facing clinical text."""

from app.rendering.response_renderer import RenderedResponse, render

__all__ = ["RenderedResponse", "render"]
