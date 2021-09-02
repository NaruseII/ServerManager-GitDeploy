package fr.naruse.servermanager.gitdeploy.main;

import fr.naruse.servermanager.core.api.events.IEvent;
import fr.naruse.servermanager.core.api.events.plugin.PluginFileManagerEvent;
import fr.naruse.servermanager.core.command.AbstractCoreCommand;
import fr.naruse.servermanager.core.plugin.SMPlugin;
import fr.naruse.servermanager.core.config.Configuration;
import fr.naruse.servermanager.gitdeploy.command.CommandDeploy;
import fr.naruse.servermanager.gitdeploy.task.CollectionsStatus;
import fr.naruse.servermanager.gitdeploy.task.GitDeployTask;
import org.eclipse.jgit.api.Git;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GitDeployPlugin extends SMPlugin {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
    private static final ExecutorService ERROR_EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    private File templateFolder;
    private Map<String, Configuration> templateByNameMap = new HashMap<>();

    public GitDeployPlugin(String name, File dataFolder) {
        super(name, dataFolder);
    }

    @Override
    public void init() {
        this.templateFolder = new File(this.getDataFolder(), "templates");
        this.templateFolder.mkdirs();

        this.reloadTemplates();
        AbstractCoreCommand.get().registerCommand("deploy", new CommandDeploy(this), "deploy <TemplateName, All, ReloadTemplates>");

        this.deployAll();
    }

    @Override
    public void shutdown() {
        this.getLogger().info("Shutting down thread pool...");
        EXECUTOR_SERVICE.shutdownNow();
        ERROR_EXECUTOR_SERVICE.shutdownNow();
        Git.shutdown();
    }

    @Override
    public void handlePluginEvent(IEvent event) {
        if(event instanceof PluginFileManagerEvent.AsyncPreCreateServerEvent){
            PluginFileManagerEvent.AsyncPreCreateServerEvent e = (PluginFileManagerEvent.AsyncPreCreateServerEvent) event;

            if(!CollectionsStatus.canStart(e.getTemplateName())){
                this.getLogger().warn("Please be patient! I'm deploying this template");
                e.setCancelled(true);
            }
        }
    }

    public void reloadTemplates(){
        this.getLogger().info("Looking for templates...");
        this.templateByNameMap.clear();

        if(this.templateFolder.listFiles() != null){
            for (File file : this.templateFolder.listFiles()) {
                if(file.getName().endsWith(".json")){
                    Configuration configuration = new Configuration(file, this.getClass().getClassLoader().getResourceAsStream("resources/template.json"));
                    this.templateByNameMap.put(file.getName().replace(".json", ""), configuration);
                }
            }
        }

        this.getLogger().info(this.templateByNameMap.size()+" templates found");
    }

    public boolean deploy(String templateName) {
        if(this.templateByNameMap.containsKey(templateName)){
            Future future = EXECUTOR_SERVICE.submit(() -> {
                new GitDeployTask(this, templateName, this.templateByNameMap.get(templateName));
            });
            ERROR_EXECUTOR_SERVICE.submit(() -> {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            });
            return true;
        }
        return false;
    }

    public void deployAll() {
        for (String templateName : new HashSet<>(this.templateByNameMap.keySet())) {
            this.deploy(templateName);
        }
    }
}
