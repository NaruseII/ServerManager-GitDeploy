package fr.naruse.servermanager.gitdeploy.task;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class CollectionsStatus {

    public static Set<String> DEPLOYING_TEMPLATES_SET = new ConcurrentSkipListSet<>();

    public static boolean canStart(String templateName){
        return !DEPLOYING_TEMPLATES_SET.contains(templateName);
    }
}
