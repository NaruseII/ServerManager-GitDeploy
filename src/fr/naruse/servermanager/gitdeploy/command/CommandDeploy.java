package fr.naruse.servermanager.gitdeploy.command;

import fr.naruse.servermanager.core.command.AbstractCoreCommand;
import fr.naruse.servermanager.gitdeploy.main.GitDeployPlugin;
import fr.naruse.servermanager.gitdeploy.task.GitDeployTask;

public class CommandDeploy implements AbstractCoreCommand.ICommand {

    private final GitDeployPlugin pl;

    public CommandDeploy(GitDeployPlugin pl) {
        this.pl = pl;
    }

    @Override
    public void onCommand(String s, String[] args) {
        if(args.length == 1){
            this.pl.getLogger().error("deploy <TemplateName, All, ReloadTemplates>");
            return;
        }

        if(args[1].equalsIgnoreCase("all")){
            this.pl.getLogger().info("Deploying...");
            this.pl.deployAll();
            return;
        }else if(args[1].equalsIgnoreCase("ReloadTemplates")){
            this.pl.reloadTemplates();
            return;
        }
        this.pl.getLogger().info("Deploying...");
        if(!this.pl.deploy(args[1])){
            this.pl.getLogger().error("Template '"+args[1]+"' not found");
        }
    }
}
