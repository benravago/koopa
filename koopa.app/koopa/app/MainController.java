package koopa.app;

import fx.mvc.View;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.Parent;
import javafx.stage.WindowEvent;

import koopa.app.actions.OpenFileAction;
import koopa.app.actions.ShowASTAction;
import koopa.app.components.tabs.TabSettings;

@View("koopa.app.MainFrame")
class MainController extends MainApp {

    WorkingSet ws;

    void onLoad(Event e) {
        System.out.println("onLoad "+System.currentTimeMillis());
        ws = new WorkingSet();
        ws.root = (Parent) e.getSource();
        new TabSettings(ws).run();
    }

    void onShowing(WindowEvent e) {
        System.out.println("onShowing "+System.currentTimeMillis());
    }

    void onShown(WindowEvent e) {
        System.out.println("onShown "+System.currentTimeMillis());
    }

    @Override
    void doExit(ActionEvent e) {
        Platform.exit();
    }

    @Override
    void doFileParse(ActionEvent e) {
        new OpenFileAction(ws).run();
    }

    @Override
    void doFileReload(ActionEvent e) {
        // ReloadFileAction.txt
    }

    @Override
    void doFileDebug(ActionEvent e) {
        // DebugAction.txt
    }

    @Override
    void doFileClose(ActionEvent e) {
        // CloseFileAction.txt
    }

    @Override
    void doQuitParsing(ActionEvent e) {
        // QuitParsingAction.txt
    }

    @Override
    void doFileClear(ActionEvent e) {
        // TODO Auto-generated method stub
    }

    @Override
    void doFileExport(ActionEvent e) {
        // ExportBatchResultsToCSVAction.txt
    }

    @Override
    void doCopybookPaths(ActionEvent e) {
        // TODO Auto-generated method stub
    }

    @Override
    void doFileExtensions(ActionEvent e) {
        // TODO Auto-generated method stub
    }

    @Override
    void doLineEndings(ActionEvent e) {
        // TODO Auto-generated method stub
    }

    @Override
    void doTabs(ActionEvent e) {
        // TODO Auto-generated method stub
    }

    @Override
    void doCobolWords(ActionEvent e) {
        // TODO Auto-generated method stub
    }

    @Override
    void doGoToLine(ActionEvent e) {
        // GoToLineAction.txt
    }

    @Override
    void doFind(ActionEvent e) {
        // FindAction.txt
    }

    @Override
    void doFindAgain(ActionEvent e) {
        // FindAgainAction.txt
    }

    @Override
    void doShowGrammar(ActionEvent e) {
        // ShowGrammarAction.txt
    }

    @Override
    void doShowAst(ActionEvent e) {
        new ShowASTAction(ws).run();
    }

    @Override
    void doExportToXml(ActionEvent e) {
        // ExportASTToXMLAction.txt
    }

    @Override
    void doFindByXpath(ActionEvent e) {
        // QueryUsingXPathAction.txt
    }

    @Override
    void doReadme(ActionEvent e) {
        // TODO Auto-generated method stub
    }

    @Override
    void doContributors(ActionEvent e) {
        // TODO Auto-generated method stub
    }

    @Override
    void doLicense(ActionEvent e) {
        // TODO Auto-generated method stub
    }

}
/*
ParsingProvider.txt
SetLogLevelAction.txt
SetSourceFormatAction.txt
ShowMarkdownAction.txt
*/
