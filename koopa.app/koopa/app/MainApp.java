package koopa.app;

import javafx.event.ActionEvent;
import javafx.scene.control.ToggleGroup;

abstract class MainApp {

  ToggleGroup toggleSourceFormat;
  ToggleGroup togglePreprocessing;
  ToggleGroup toggleGrammar;
  ToggleGroup toggleLineSeparation;
  ToggleGroup toggleSourceFormatting;
  ToggleGroup toggleCompilerDirectives;
  ToggleGroup toggleSourceListingDirectives;
  ToggleGroup toggleProgramArea;
  ToggleGroup toggleCopyStatements;
  ToggleGroup toggleReplaceStatements;
  ToggleGroup toggleReplacements;
  ToggleGroup toggleLineContinutations;
  ToggleGroup toggleInlineComments;
  ToggleGroup toggleTokenSeparation;

  abstract void doExit(ActionEvent e);

  abstract void doFileParse(ActionEvent e);
  abstract void doFileReload(ActionEvent e);
  abstract void doFileDebug(ActionEvent e);
  abstract void doFileClose(ActionEvent e);
  abstract void doQuitParsing(ActionEvent e);
  abstract void doFileClear(ActionEvent e);
  abstract void doFileExport(ActionEvent e);
  abstract void doCopybookPaths(ActionEvent e);
  abstract void doFileExtensions(ActionEvent e);
  abstract void doLineEndings(ActionEvent e);
  abstract void doTabs(ActionEvent e);
  abstract void doCobolWords(ActionEvent e);
  abstract void doGoToLine(ActionEvent e);
  abstract void doFind(ActionEvent e);
  abstract void doFindAgain(ActionEvent e);
  abstract void doShowGrammar(ActionEvent e);
  abstract void doShowAst(ActionEvent e);
  abstract void doExportToXml(ActionEvent e);
  abstract void doFindByXpath(ActionEvent e);
  abstract void doReadme(ActionEvent e);
  abstract void doContributors(ActionEvent e);
  abstract void doLicense(ActionEvent e);

}
