package codeOrchestra.colt.js.plugin.actions.run;

import codeOrchestra.colt.core.ColtFacade;
import codeOrchestra.colt.core.rpc.ColtRemoteServiceProvider;
import codeOrchestra.colt.core.rpc.model.ColtLauncherType;
import codeOrchestra.colt.js.plugin.ProjectSettings;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import utils.FileUtils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Alexander Eliseyev
 */
public class JsRunWithColtAction extends JsRunWithColt {

    private static final Pattern META_PATTERN = Pattern.compile("<meta[^>]*>");
    private static final Pattern META_NAME = Pattern.compile("name=\"([^\"]+)");
    private static final Pattern META_CONTENT = Pattern.compile("content=\"([^\"]+)");

    public JsRunWithColtAction() {
        myLauncherType = ColtLauncherType.BROWSER;
    }

    @Override
    public void actionPerformed(AnActionEvent actionEvent) {
        VirtualFile[] virtualFileArray = (VirtualFile[]) actionEvent.getDataContext().getData("virtualFileArray");

        if (virtualFileArray == null || virtualFileArray[0] == null) {
            throw new IllegalStateException(); // should not happen
        }

        String path = virtualFileArray[0].getPath().toLowerCase();
        Project project = actionEvent.getProject();
        if(project == null) {
            return;
        }

        ProjectSettings.State state = project.getComponent(ProjectSettings.class).getState();

        if(!(path.endsWith(".html") || path.endsWith(".htm"))) {
            // check connection
            if (project.getComponent(ColtRemoteServiceProvider.class).isConnected() && state != null && state.lastLauncherType == myLauncherType) {
                project.getComponent(ColtFacade.class).startLive();
            // check state
            } else if(state != null && state.lastLauncherType == myLauncherType && !"".equals(state.projectPath)) {
                if(!runExistsConfiguration(project, state.runConfigurationName)) {
                    Notifications.Bus.notify(new Notification("colt.notification", "COLT", "Main document for 'Run With COLT' can be only HTML file.", NotificationType.ERROR));
                }
            } else {
                Notifications.Bus.notify(new Notification("colt.notification", "COLT", "Main document for 'Run With COLT' can be only HTML file.", NotificationType.ERROR));
            }
        } else {
            String mainDocumentPath = virtualFileArray[0].getPath();
            String mainDocumentName = virtualFileArray[0].getName();

            String content = FileUtils.read(new File(mainDocumentPath));
            Matcher m = META_PATTERN.matcher(content);
            while(m.find()) {
                String group = m.group(0);
                Matcher matcher = META_NAME.matcher(group);
                if(matcher.find()) {
                    String name = matcher.group(1);
                    System.out.println("name = " + name);
                    if("colt-main-document".equals(name)) {
                        Matcher matcher1 = META_CONTENT.matcher(group);
                        if(matcher1.find()) {
                            mainDocumentPath = File.separator + matcher1.group(1);
                        }
                    }
                }
            }

            runWithColt(actionEvent, mainDocumentPath, mainDocumentName, "autogenerated");
        }
    }

}
