package org.yli.littlespy.ui

import com.google.gson.Gson
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.AnchorPane
import javafx.stage.FileChooser
import org.apache.commons.io.FileUtils
import org.yli.littlespy.domain.FrameInfo
import org.yli.littlespy.domain.StackMemory
import org.yli.littlespy.domain.ThreadInfo
import org.yli.littlespy.domain.VariableInfo
import org.yli.littlespy.ui.data.ThreadStackItem
import tornadofx.View
import java.nio.charset.Charset
import kotlin.concurrent.thread

/**
 * Created by yli on 12/23/2016.
 */
class LittleSpyStackViewer : View() {
    override val root : AnchorPane by fxml("/views/viewer.fxml")
    val threadTree : TreeView<ThreadStackItem> by fxid()
    val variableTable : TableView<VariableInfo> by fxid()
    val varCol : TableColumn<VariableInfo, String> by fxid()
    val varTypeCol : TableColumn<VariableInfo, String> by fxid()
    val valueText : TextArea by fxid()

    init {
        title = "Stack Memory Viewer"

        threadTree.selectionModel.selectedItemProperty().addListener { observableValue, oldItem, newItem ->
            val value = newItem.value
            if (value is ThreadStackItem) {
                if (value.hasVariables && value.frameInfo != null) {
                    variableTable.items = createVariableList(value.frameInfo)
                } else {
                    variableTable.items = FXCollections.observableArrayList()
                }
            }
        }

        varCol.cellValueFactory = PropertyValueFactory<VariableInfo, String>("name")
        varTypeCol.cellValueFactory = PropertyValueFactory<VariableInfo, String>("type")

        variableTable.selectionModel.selectedItemProperty().addListener { observableValue, oldValue, newValue ->
            if (newValue is VariableInfo) {
                valueText.text = newValue.value
            } else {
                valueText.text = ""
            }
        }
    }

    private fun createVariableList(frameInfo: FrameInfo): ObservableList<VariableInfo> {
        val resultList = FXCollections.observableArrayList<VariableInfo>()

        for (variable in frameInfo.variables) {
            resultList.add(variable)
        }

        return resultList
    }

    /**
     * Call back for loading memory stack.
     */
    fun doLoad() {
        val fileChooser = FileChooser()
        fileChooser.title = "Choose the JSON file"
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("JSON File (*.json)", "*.json"))
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("All File (*.*)", "*.*"))

        val selectedFile = fileChooser.showOpenDialog(primaryStage)
        if (selectedFile != null) {
            val stackMemory =
                    Gson().fromJson(FileUtils.readFileToString(selectedFile, Charset.defaultCharset()),
                                    StackMemory::class.java)
            if (stackMemory != null) {
                doUpdateThreadTree(stackMemory)
            }
        }
    }

    private fun doUpdateThreadTree(stackMemory: StackMemory?) {
        if (stackMemory == null) {
            return
        }

        val root = TreeItem<ThreadStackItem>(ThreadStackItem("Threads"))
        root.isExpanded = true

        // val threadsList = FXCollections.observableArrayList<TreeItem<ThreadStackItem>>()
        for (threadInfo in stackMemory.threads) {
            val threadNode = TreeItem<ThreadStackItem>(ThreadStackItem(threadInfo.name, false))

            for (frame in threadInfo.frames) {
                val frameNode =
                        TreeItem<ThreadStackItem>(
                                ThreadStackItem(
                                        "${frame.methodName} (${frame.sourcePath}:${frame.lineNumber})",
                                        true, frame))
                threadNode.children.add(frameNode)
            }
            root.children.add(threadNode)
        }

        threadTree.root = root
    }
}


