package org.example.agent.skill.service;

import org.example.agent.skill.model.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SkillSelector {

    private static final Logger logger = LoggerFactory.getLogger(SkillSelector.class);

    private final SkillRegistry skillRegistry;

    public SkillSelector(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
    }

    public Optional<Skill> selectByAlarmType(String alarmType) {
        List<Skill> allSkills = skillRegistry.getAllSkills();

        return allSkills.stream()
                .filter(Skill::isEnabled)
                .filter(skill -> skill.getApplicableScenarios() != null &&
                        skill.getApplicableScenarios().stream()
                                .anyMatch(scenario -> alarmType.contains(scenario) || scenario.contains(alarmType)))
                .max(Comparator.comparingInt(Skill::getPriority));
    }

    public Optional<Skill> selectByIntent(String intent) {
        List<Skill> allSkills = skillRegistry.getAllSkills();

        return allSkills.stream()
                .filter(Skill::isEnabled)
                .filter(skill -> skill.getApplicableScenarios() != null &&
                        skill.getApplicableScenarios().stream()
                                .anyMatch(scenario -> intent.contains(scenario) || scenario.contains(intent)))
                .max(Comparator.comparingInt(Skill::getPriority));
    }

    public List<Skill> selectByCategory(String category) {
        return skillRegistry.getSkillsByCategory(category);
    }
}
