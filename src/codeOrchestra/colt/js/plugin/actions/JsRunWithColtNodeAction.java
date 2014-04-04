package codeOrchestra.colt.js.plugin.actions;

import codeOrchestra.colt.core.plugin.ColtSettings;
import codeOrchestra.colt.core.plugin.icons.Icons;
import codeOrchestra.colt.core.rpc.model.ColtLauncherType;
import codeOrchestra.colt.js.plugin.controller.JsColtPluginController;
import codeOrchestra.colt.js.plugin.run.JsColtConfigurationFactory;
import codeOrchestra.colt.js.plugin.run.JsColtConfigurationType;
import codeOrchestra.colt.js.plugin.run.JsColtRunConfiguration;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author Alexander Eliseyev
 */
public class JsRunWithColtNodeAction extends AnAction {

    public JsRunWithColtNodeAction() {
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);

        e.getPresentation().setIcon(Icons.COLT_ICON_16);

        if (e.getProject() == null) {
            e.getPresentation().setEnabled(false);
        }

        VirtualFile[] virtualFileArray = (VirtualFile[]) e.getDataContext().getData("virtualFileArray");
        if (virtualFileArray != null && virtualFileArray.length == 1 && !virtualFileArray[0].isDirectory() && !"".equals(ColtSettings.getInstance().getNodePath()) &&
                (virtualFileArray[0].getPath().toLowerCase().endsWith(".js"))) {
            e.getPresentation().setEnabled(true);
        } else {
            e.getPresentation().setEnabled(false);
        }
    }

    @Override
    public void actionPerformed(AnActionEvent actionEvent) {
        VirtualFile[] virtualFileArray = (VirtualFile[]) actionEvent.getDataContext().getData("virtualFileArray");

        if (virtualFileArray == null || virtualFileArray[0] == null) {
            throw new IllegalStateException(); // should not happen
        }

        String mainDocumentPath = virtualFileArray[0].getPath();
        String mainDocumentName = virtualFileArray[0].getName();

        runWithColt(actionEvent, mainDocumentPath, mainDocumentName, "autogenerated");
    }

    private void runWithColt(AnActionEvent actionEvent, String mainDocumentPath, String mainDocumentName, String runConfigurationName) {
        // 0 - check if such configuration already exists
        RunManager runManager = RunManager.getInstance(actionEvent.getProject());
        for (RunnerAndConfigurationSettings runnerAndConfigurationSettings : runManager.getConfigurationSettings(getColtConfigurationType(runManager))) {
            if (runnerAndConfigurationSettings.getName().equals(runConfigurationName)) {
                if (runnerAndConfigurationSettings.getConfiguration() instanceof JsColtRunConfiguration) {
                    ProgramRunnerUtil.executeConfiguration(actionEvent.getProject(), runnerAndConfigurationSettings, DefaultRunExecutor.getRunExecutorInstance());
                    return;
                } else {
                    runWithColt(actionEvent, mainDocumentPath, mainDocumentName, runConfigurationName + " (1)");
                    return;
                }
            }
        }

        // 1 - export project
        String projectPath = JsColtPluginController.export(actionEvent.getProject(), mainDocumentName, mainDocumentPath, ColtLauncherType.NODE_JS);

        // 2 - create a run configuration
        JsColtConfigurationType coltConfigurationType = getColtConfigurationType(runManager);
        if (coltConfigurationType == null) {
            throw new IllegalStateException("Can't locate COLT configuration type");
        }
        JsColtConfigurationFactory coltConfigurationFactory = null;
        for (ConfigurationFactory configurationFactory : coltConfigurationType.getConfigurationFactories()) {
            if (configurationFactory instanceof JsColtConfigurationFactory) {
                coltConfigurationFactory = (JsColtConfigurationFactory) configurationFactory;
                break;
            }
        }
        if (coltConfigurationFactory == null) {
            throw new IllegalStateException("Can't locate COLT configuration factory");
        }
        RunnerAndConfigurationSettings runConfiguration = runManager.createRunConfiguration(runConfigurationName, coltConfigurationFactory);
        JsColtRunConfiguration JsColtRunConfiguration = (JsColtRunConfiguration) runConfiguration.getConfiguration();
        JsColtRunConfiguration.setColtProjectPath(projectPath);
        if (runManager instanceof RunManagerEx) {
            runManager.addConfiguration(runConfiguration, false);
            runManager.setSelectedConfiguration(runConfiguration);
        }

        // 3 - run configuration
        ProgramRunnerUtil.executeConfiguration(actionEvent.getProject(), runConfiguration, DefaultRunExecutor.getRunExecutorInstance());
    }

    private JsColtConfigurationType getColtConfigurationType(RunManager runManager) {
        JsColtConfigurationType coltConfigurationType = null;
        for (ConfigurationType configurationType : runManager.getConfigurationFactories()) {
            if (configurationType instanceof JsColtConfigurationType) {
                coltConfigurationType = (JsColtConfigurationType) configurationType;
                break;
            }
        }
        return coltConfigurationType;
    }

}
