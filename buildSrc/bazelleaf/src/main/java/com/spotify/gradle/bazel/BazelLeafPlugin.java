package com.spotify.gradle.bazel;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.Exec;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class BazelLeafPlugin implements Plugin<Project> {

    public void apply(final Project project) {
        project.getExtensions().create("bazel", BazelLeafConfig.class);

        project.afterEvaluate(BazelLeafPlugin::configurePlugin);
    }

    private static void configurePlugin(Project project) {
        final BazelLeafConfig.Decorated config = project.getExtensions().getByType(BazelLeafConfig.class).decorate(project);

        final Project rootProject = project.getRootProject();

        final AspectRunner aspectRunner = new AspectRunner(config);
        final Strategy strategy = Strategy.buildStrategy(aspectRunner.getAspectResult("get_rule_kind.bzl").stream().findFirst().orElse("java_library"), config);
        /*
         * creating a Bazel-Build task
         */
        //note: Bazel must use the same folder for all outputs, so we use the build-folder of the root-project
        final Exec bazelBuildTask = strategy.createBazelBuildTask(project);

        /*
         * Adding build configurations
         */
        final Configuration defaultConfiguration = project.getConfigurations().create(Dependency.DEFAULT_CONFIGURATION);
        defaultConfiguration.setCanBeConsumed(true);
        defaultConfiguration.setCanBeResolved(true);

        /*
         * Adding build artifacts
         */
        strategy.getBazelArtifacts(aspectRunner, bazelBuildTask).forEach(bazelPublishArtifact -> defaultConfiguration.getOutgoing().getArtifacts().add(bazelPublishArtifact));

        //ConfigurationVariant variant = .add(
        //variant.getAttributes().attribute(Usage.USAGE_ATTRIBUTE, rootProject.getObjects().named(Usage.class, Usage.JAVA_API_CLASSES));
        //variant.artifact(bazelArtifact);

        /*
         * Applying IDEA plugin, so InteliJ will index the source files
         */
        IdeaPlugin ideaPlugin = (IdeaPlugin) project.getPlugins().apply("idea");
        final IdeaModule ideaModule = ideaPlugin.getModel().getModule();
        ideaModule.setSourceDirs(getSourceFoldersFromBazelAspect(rootProject, aspectRunner));

        /*
         * Creating a CLEAN task in the root project
         */
        if (rootProject.getTasksByName("bazelClean", false/*only search the root project*/).isEmpty()) {
            final Exec bazelCleanTask = (Exec) rootProject.task(Collections.singletonMap("type", Exec.class), "bazelClean");
            bazelCleanTask.setWorkingDir(config.workspaceRootFolder);
            bazelCleanTask.setCommandLine(config.bazelBin, "clean", "--symlink_prefix=" + config.buildOutputDir);

            rootProject.getTasks().findByPath(":clean").dependsOn(bazelCleanTask);
        }
    }

    private static List<String> getModuleDepsFromBazel(Project rootProject, AspectRunner aspectRunner) {
        final Pattern pattern = Pattern.compile("^<target.*//(.+):.*>$");
        return aspectRunner.getAspectResult("get_deps.bzl").stream()
                .map(pattern::matcher)
                .filter(Matcher::matches)
                .map(matcher -> matcher.group(1))
                .map(bazelDep -> ":" + bazelDep.replace("/", ":"))
                .collect(Collectors.toList());
    }

    private static Set<File> getSourceFoldersFromBazelAspect(Project rootProject, AspectRunner runner) {
        final Map<File, String> packageByFolder = new HashMap<>();

        return runner.getAspectResult("get_source_files.bzl").stream()
                .map(File::new)
                //we need the root-project since the WORKSPACE file is there.
                .map(rootProject::file)
                .map(sourceFile -> {
                    File parent = sourceFile.getParentFile();
                    String packageInFolder = packageByFolder.computeIfAbsent(parent, fileNotUsedHere -> parseDeclaredPackage(sourceFile));
                    final String parentFullPath = parent.getPath();
                    //removing the package folders, we only want the root folder
                    return new File(parentFullPath.substring(0, parentFullPath.length() - packageInFolder.length()));
                })
                .distinct()
                .collect(Collectors.toSet());
    }

    //taken from https://github.com/bazelbuild/intellij/blob/master/aspect/tools/src/com/google/idea/blaze/aspect/PackageParser.java#L163
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([\\w\\.]+);$");

    @Nullable
    private static String parseDeclaredPackage(File sourceFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher packageMatch = PACKAGE_PATTERN.matcher(line);
                if (packageMatch.find()) {
                    return packageMatch.group(1);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse java package for " + sourceFile, e);
        }
        return null;
    }
}
