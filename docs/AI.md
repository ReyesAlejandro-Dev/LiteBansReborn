# 🧠 AI Integration Guide

LiteBansReborn v5.0 introduces **Antigravity AI**, a powerful system that leverages external AI models to assist with moderation.

## Features
* **Toxicity Detection**: Analyzes chat messages for toxicity, hate speech, and harassment in real-time.
* **Appeal Review**: AI assistant analyzes ban appeals and provides a recommendation (Accept/Deny) based on sincerity and history.
* **Behavior Analysis**: (Coming Soon) Detects patterns of rule-breaking behavior.

> **Note**: This feature uses external APIs. No local server resources (CPU/RAM) are used for inference.

## Supported Providers
You can choose from multiple AI providers:
* **OpenRouter** (Recommended, access to all models)
* **Venice.ai** (Privacy-focused, uncensored)
* **OpenAI** (GPT-4, GPT-3.5)
* **DeepSeek** (Cost-effective)
* **Anthropic** (Claude 3.5 Sonnet/Haiku)

## Configuration

Located in `config.yml`:

```yaml
ai:
  enabled: true
  provider: "openrouter" # openrouter, venice, openai, etc.
  api-key: "sk-or-..."   # Your API Key
  model: "deepseek/deepseek-chat" # Model ID
  
  features:
    toxicity-detection: true
    appeal-review: true
```

## Privacy
We respect player privacy. Only specific data (chat messages for checking, appeal text) is sent to the API.
- If using **Venice.ai**, your data is processed privately.
- If using **OpenRouter/OpenAI**, check their respective privacy policies.
