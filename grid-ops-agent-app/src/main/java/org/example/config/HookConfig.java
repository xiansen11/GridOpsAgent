package org.example.config;

import org.example.hook.AgentHook;
import org.example.hook.HookEngine;
import org.example.hook.impl.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class HookConfig {

    @Autowired
    private HookEngine hookEngine;

    @Autowired
    private PreRouteHook preRouteHook;

    @Autowired
    private PostRouteHook postRouteHook;

    @Autowired
    private PreRagHook preRagHook;

    @Autowired
    private PostRagHook postRagHook;

    @Autowired
    private PreToolUseHook preToolUseHook;

    @Autowired
    private PostToolUseHook postToolUseHook;

    @Autowired
    private PreDiagnosisHook preDiagnosisHook;

    @Autowired
    private PostDiagnosisHook postDiagnosisHook;

    @Autowired
    private AuditHook auditHook;

    @Autowired
    private DataMaskingHook dataMaskingHook;

    @Autowired
    private HumanApprovalHook humanApprovalHook;

    @Autowired
    private SafetyCheckHook safetyCheckHook;

    @PostConstruct
    public void registerHooks() {
        hookEngine.registerHook("PRE_ROUTE", auditHook);
        hookEngine.registerHook("PRE_ROUTE", preRouteHook);

        hookEngine.registerHook("POST_ROUTE", auditHook);
        hookEngine.registerHook("POST_ROUTE", postRouteHook);

        hookEngine.registerHook("PRE_RAG", auditHook);
        hookEngine.registerHook("PRE_RAG", preRagHook);

        hookEngine.registerHook("POST_RAG", auditHook);
        hookEngine.registerHook("POST_RAG", postRagHook);

        hookEngine.registerHook("PRE_TOOL_USE", auditHook);
        hookEngine.registerHook("PRE_TOOL_USE", preToolUseHook);
        hookEngine.registerHook("PRE_TOOL_USE", safetyCheckHook);

        hookEngine.registerHook("POST_TOOL_USE", auditHook);
        hookEngine.registerHook("POST_TOOL_USE", postToolUseHook);
        hookEngine.registerHook("POST_TOOL_USE", dataMaskingHook);

        hookEngine.registerHook("PRE_DIAGNOSIS", auditHook);
        hookEngine.registerHook("PRE_DIAGNOSIS", preDiagnosisHook);

        hookEngine.registerHook("POST_DIAGNOSIS", auditHook);
        hookEngine.registerHook("POST_DIAGNOSIS", postDiagnosisHook);
        hookEngine.registerHook("POST_DIAGNOSIS", safetyCheckHook);
        hookEngine.registerHook("POST_DIAGNOSIS", humanApprovalHook);
    }
}
