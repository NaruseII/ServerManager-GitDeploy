package fr.naruse.servermanager.gitdeploy.task;

import fr.naruse.servermanager.core.config.Configuration;
import fr.naruse.servermanager.core.logging.ServerManagerLogger;
import fr.naruse.servermanager.core.plugin.SMPlugin;
import fr.naruse.servermanager.core.utils.Utils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;

public class GitDeployTask {

    private final ServerManagerLogger.Logger LOGGER;

    public GitDeployTask(SMPlugin pl, String templateName, Configuration configuration) {
        this.LOGGER  = new ServerManagerLogger.Logger("GitDeploy - "+templateName);
        if(!(boolean) configuration.get("enabled")){
            return;
        }

        LOGGER.info("Launching new task...");

        Configuration template = pl.getServerManager().getConfigurationManager().getTemplate(templateName);
        if(template == null){
            LOGGER.error("True template '"+templateName+".json' not found!");
            return;
        }

        CollectionsStatus.DEPLOYING_TEMPLATES_SET.add(templateName);
        String templateFolderUrl = template.get("pathTemplate");
        LOGGER.debug("Template folder URL is '"+templateFolderUrl+"'");
        File templateFolder = new File(templateFolderUrl);

        boolean pull = new File(templateFolder, ".git").exists();
        if(!pull){
            Utils.delete(templateFolder);
            LOGGER.info(".git not found! Cloning...");
        }else{
            LOGGER.info(".git found! Checkout...");
        }

        Configuration.ConfigurationSection credentialsSection = configuration.getSection("credentials");
        String url = configuration.get("url");
        String branch = configuration.get("branch");
        String username = credentialsSection.get("username");
        String password = credentialsSection.get("password");
        boolean useCredentials = credentialsSection.get("enabled");

        LOGGER.info("Repository location is '"+url+"'");

        LOGGER.info("Downloading...");
        try {
            if(pull){
                PullCommand pullCommand = Git.open(templateFolder)
                         .pull();
                if(useCredentials){
                    pullCommand = pullCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
                }
                pullCommand.call();
            }else{
                CloneCommand cloneCommand = Git.cloneRepository()
                        .setURI(url)
                        .setDirectory(templateFolder);
                if(useCredentials){
                    cloneCommand = cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
                }
                cloneCommand.call();
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Task failed");
            CollectionsStatus.DEPLOYING_TEMPLATES_SET.remove(templateName);
            return;
        }

        CollectionsStatus.DEPLOYING_TEMPLATES_SET.remove(templateName);
        LOGGER.info("Task complete");
    }
}
