package org.example.agent.skill.service;

import jakarta.annotation.PostConstruct;
import org.example.agent.skill.model.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SkillRegistry {

    private static final Logger logger = LoggerFactory.getLogger(SkillRegistry.class);
    private final Map<String, Skill> skills = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        registerBuiltinSkills();
        logger.info("Skill Registry 初始化完成，已注册 {} 个 Skill", skills.size());
    }

    public void register(Skill skill) {
        skill.setCreatedAt(LocalDateTime.now());
        skill.setUpdatedAt(LocalDateTime.now());
        skills.put(skill.getSkillId(), skill);
        logger.info("注册Skill: skillId={}, name={}", skill.getSkillId(), skill.getName());
    }

    public Optional<Skill> getSkill(String skillId) {
        return Optional.ofNullable(skills.get(skillId));
    }

    public List<Skill> getAllSkills() {
        return new ArrayList<>(skills.values());
    }

    public List<Skill> getEnabledSkills() {
        return skills.values().stream()
                .filter(Skill::isEnabled)
                .collect(Collectors.toList());
    }

    public List<Skill> getSkillsByCategory(String category) {
        return skills.values().stream()
                .filter(skill -> category.equals(skill.getCategory()))
                .filter(Skill::isEnabled)
                .collect(Collectors.toList());
    }

    public void unregister(String skillId) {
        skills.remove(skillId);
    }

    private void registerBuiltinSkills() {
        register(Skill.builder()
                .skillId("transformer-oil-temp-diagnosis")
                .name("主变油温异常诊断")
                .version("1.0")
                .description("针对主变压器油温异常告警的诊断流程，包括冷却系统检查、负荷分析、历史缺陷关联等")
                .category("fault_diagnosis")
                .priority(80)
                .applicableScenarios(List.of("油温异常", "油温升高", "冷却器异常", "变压器过热"))
                .recommendedTools(List.of("getDeviceStatus", "getAlarmHistory", "getDeviceLogs", "getDefectTickets", "searchSafetyRules", "getDeviceProfile"))
                .diagnosisWorkflow(List.of("查设备状态→查冷却器状态→查环境温度→查负荷→查历史告警→查缺陷工单→查安规→生成诊断报告"))
                .promptTemplate("你正在执行主变油温异常诊断流程。请按步骤排查：1.确认油温读数 2.检查冷却器运行状态 3.检查负荷情况 4.检查环境温度 5.查看历史告警 6.查看缺陷工单 7.查询相关安规 8.生成诊断报告")
                .examples(List.of(
                        Map.of("input", "1号主变油温86℃超过阈值80℃", "output", "执行油温异常诊断流程，检查冷却器#2风机启动失败..."),
                        Map.of("input", "主变TR-110KV-001油温持续升高", "output", "检查冷却系统、负荷情况、历史缺陷...")
                ))
                .build());

        register(Skill.builder()
                .skillId("switchgear-pd-diagnosis")
                .name("开关柜局放异常诊断")
                .version("1.0")
                .description("针对开关柜局部放电异常告警的诊断流程")
                .category("fault_diagnosis")
                .priority(80)
                .applicableScenarios(List.of("局放异常", "局部放电", "开关柜放电", "绝缘异常"))
                .recommendedTools(List.of("getDeviceStatus", "getAlarmHistory", "getDefectTickets", "getDeviceProfile"))
                .diagnosisWorkflow(List.of("查设备状态→查局放值→查历史告警→查缺陷工单→查设备台账→生成诊断报告"))
                .promptTemplate("你正在执行开关柜局放异常诊断流程。请按步骤排查：1.确认局放数值 2.查看局放趋势 3.检查历史告警 4.检查缺陷工单 5.查看设备台账 6.生成诊断报告")
                .build());

        register(Skill.builder()
                .skillId("safety-regulation-qa")
                .name("安规条款查询")
                .version("1.0")
                .description("电力安全工作规程查询和解读")
                .category("knowledge_qa")
                .priority(70)
                .applicableScenarios(List.of("安规", "安全规程", "操作规程", "安全措施", "高压室", "倒闸操作"))
                .recommendedTools(List.of("searchSafetyRules", "queryInternalDocs"))
                .promptTemplate("请查询相关安规条款，给出准确引用和安全建议。")
                .build());

        register(Skill.builder()
                .skillId("defect-ticket-check")
                .name("缺陷工单检查")
                .version("1.0")
                .description("历史缺陷工单查询和重复缺陷判断")
                .category("ticket_analysis")
                .priority(60)
                .applicableScenarios(List.of("缺陷工单", "历史缺陷", "重复缺陷", "维修记录"))
                .recommendedTools(List.of("getDefectTickets", "getAlarmHistory"))
                .promptTemplate("请查询设备历史缺陷工单，分析与当前告警的关联性，判断是否为重复缺陷。")
                .build());

        register(Skill.builder()
                .skillId("line-trip-repair")
                .name("配网线路跳闸抢修")
                .version("1.0")
                .description("配网线路跳闸告警的抢修指导流程")
                .category("fault_diagnosis")
                .priority(75)
                .applicableScenarios(List.of("线路跳闸", "配网故障", "停电", "线路故障"))
                .recommendedTools(List.of("getDeviceStatus", "getAlarmHistory", "getDeviceLogs", "searchSafetyRules"))
                .promptTemplate("请执行配网线路跳闸抢修流程：1.确认跳闸线路和范围 2.查询保护动作信息 3.查询历史告警 4.制定抢修方案 5.安全措施提示")
                .build());
    }
}
