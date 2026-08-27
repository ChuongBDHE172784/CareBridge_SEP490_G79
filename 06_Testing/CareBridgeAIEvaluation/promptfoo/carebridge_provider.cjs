'use strict';

const { spawnSync } = require('node:child_process');

class CareBridgeProvider {
  id() {
    return 'carebridge-deterministic-or-conversation-api';
  }

  async callApi(prompt, context) {
    const vars = (context && context.vars) || {};
    if ((vars.executionMode || 'LOCAL_DETERMINISTIC') === 'LOCAL_DETERMINISTIC') {
      return this.callLocal(vars);
    }
    return this.callConversationApi(prompt, vars);
  }

  callLocal(vars) {
    const python = process.env.PYTHON || (process.platform === 'win32' ? 'python' : 'python3');
    const completed = spawnSync(
      python,
      ['-m', 'carebridge_evaluation.cli', 'promptfoo-local'],
      {
        encoding: 'utf8',
        input: JSON.stringify({
          stage: vars.stage,
          category: vars.category,
          subcategory: vars.subcategory,
          currentIntake: vars.currentIntake || {},
        }),
        env: Object.assign({}, process.env, { GEMINI_ENABLED: 'false', PYTHONUTF8: '1' }),
        maxBuffer: 1024 * 1024,
      },
    );
    if (completed.error) {
      return { error: `Local deterministic provider failed: ${completed.error.name}` };
    }
    if (completed.status !== 0) {
      return { error: `Local deterministic provider exited ${completed.status}: ${(completed.stderr || '').slice(0, 500)}` };
    }
    return { output: completed.stdout.trim() };
  }

  async callConversationApi(prompt, vars) {
    const baseUrl = (process.env.CAREBRIDGE_API_BASE_URL || '').replace(/\/$/, '');
    const jwt = process.env.CAREBRIDGE_TEST_JWT || '';
    const stage = vars.stage;
    if (!baseUrl || !jwt) {
      return { error: 'INFRASTRUCTURE_SKIPPED: CAREBRIDGE_API_BASE_URL and CAREBRIDGE_TEST_JWT are required' };
    }
    if (!stage) {
      return { error: 'Benchmark stage must be explicit' };
    }
    const pediatric = stage === 'INFANT' || stage === 'TODDLER';
    const profileKey = pediatric ? 'babyProfileId' : 'motherProfileId';
    const profileId = pediatric
      ? process.env.CAREBRIDGE_TEST_BABY_PROFILE_ID
      : process.env.CAREBRIDGE_TEST_MOTHER_PROFILE_ID;
    if (!profileId) {
      return { error: `INFRASTRUCTURE_SKIPPED: ${profileKey} fixture is required` };
    }
    const currentIntake = Object.assign({}, vars.currentIntake || {}, { stage, [profileKey]: profileId });
    const controller = new AbortController();
    const timeoutMs = Number(process.env.EVALUATION_REQUEST_TIMEOUT_SECONDS || '20') * 1000;
    const timeout = setTimeout(() => controller.abort(), timeoutMs);
    try {
      const response = await fetch(`${baseUrl}/api/v1/triage/intake/conversation/start`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${jwt}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          clientRequestId: `promptfoo_${crypto.randomUUID().replaceAll('-', '')}`,
          initialText: vars.initialText || prompt,
          stage,
          [profileKey]: profileId,
          currentIntake,
        }),
        signal: controller.signal,
      });
      const body = await response.text();
      if (!response.ok) {
        return { error: `CareBridge HTTP ${response.status}: ${body.slice(0, 500)}` };
      }
      return { output: body };
    } catch (error) {
      return { error: `CareBridge request failed: ${error.name}` };
    } finally {
      clearTimeout(timeout);
    }
  }
}

module.exports = CareBridgeProvider;
