package csc207.todo_list;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TodoListPanel extends JPanel implements ActionListener {

    public static final String DONE = " (done)";
    public static final String SAVE_DIR = "saves";
    public static final String SAVEFILE_TODO_LIST_JSON =
            SAVE_DIR + File.separator + "todo_list.json";

    private final JTextField textField;
    private final DefaultListModel<String> textModel;
    private final JList<String> textList;

    public TodoListPanel() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Text field
        textField = new JTextField(20);
        textField.addActionListener(this);

        // List model
        textModel = new DefaultListModel<>();
        loadJsonFromFile();

        // Actual JList
        textList = new JList<>(textModel);

        JScrollPane scrollPane = new JScrollPane(textList);

        // Only allow one item to be selected
        ListSelectionModel listSelectionModel = textList.getSelectionModel();
        listSelectionModel.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        // When an item is selected, put its text into the text field
        listSelectionModel.addListSelectionListener(
                new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (!e.getValueIsAdjusting()) {
                            selectItem();
                        }
                    }
                }
        );

        // Keyboard controls for the list
        textList.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {

                if (evt.getKeyCode() == KeyEvent.VK_DELETE
                        || evt.getKeyCode() == KeyEvent.VK_BACK_SPACE) {

                    deleteItem();

                } else if (evt.getKeyCode() == KeyEvent.VK_SPACE) {

                    toggleDone();
                }
            }
        });

        // Save button
        JButton saveButton = new JButton("Save");

        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
            }
        });

        add(textField);
        add(scrollPane);
        add(saveButton);
    }

    // --------------------------------------------------
    // LOAD
    // --------------------------------------------------

    private void loadJsonFromFile() {
        ensureJsonExists();

        JSONArray jsonArray = readJsonFile();

        for (int i = 0; i < jsonArray.length(); i++) {

            JSONObject jsonObject = jsonArray.getJSONObject(i);

            String task = jsonObject.getString("task");
            boolean completed =
                    jsonObject.getBoolean("completed");

            if (completed) {
                task += DONE;
            }

            textModel.addElement(task);
        }
    }

    private static void ensureJsonExists() {

        Path resourcesDir = Paths.get(SAVE_DIR);

        if (!Files.exists(resourcesDir)) {
            try {
                Files.createDirectories(resourcesDir);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to create save file directory",
                        e
                );
            }
        }

        Path jsonFile =
                Paths.get(SAVEFILE_TODO_LIST_JSON);

        if (!Files.exists(jsonFile)) {
            try {
                Files.createFile(jsonFile);
                Files.write(
                        jsonFile,
                        "[]".getBytes()
                );
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to create todo_list.json file",
                        e
                );
            }
        }
    }

    private JSONArray readJsonFile() {

        String jsonString;

        try {
            jsonString = Files.readString(
                    Paths.get(SAVEFILE_TODO_LIST_JSON)
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new JSONArray(jsonString);
    }

    // --------------------------------------------------
    // SAVE
    // --------------------------------------------------

    private void save() {

        JSONArray jsonArray = new JSONArray();

        for (int i = 0; i < textModel.size(); i++) {

            JSONObject jsonObject = new JSONObject();

            String item =
                    textModel.getElementAt(i);

            jsonObject.put(
                    "task",
                    item.replace(DONE, "").trim()
            );

            jsonObject.put(
                    "completed",
                    item.endsWith(DONE)
            );

            jsonArray.put(jsonObject);
        }

        try {
            FileWriter fileWriter =
                    new FileWriter(
                            SAVEFILE_TODO_LIST_JSON
                    );

            fileWriter.write(
                    jsonArray.toString()
            );

            fileWriter.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // --------------------------------------------------
    // TOGGLE DONE
    // --------------------------------------------------

    private void toggleDone() {

        int selectedIndex =
                textList.getSelectedIndex();

        if (selectedIndex != -1) {

            String selectedText =
                    textModel.getElementAt(
                            selectedIndex
                    );

            if (selectedText.endsWith(DONE)) {

                selectedText =
                        selectedText.substring(
                                0,
                                selectedText.length()
                                        - DONE.length()
                        );

            } else {

                selectedText =
                        selectedText + DONE;
            }

            textModel.setElementAt(
                    selectedText,
                    selectedIndex
            );

            save();
        }
    }

    // --------------------------------------------------
    // DELETE
    // --------------------------------------------------

    private void deleteItem() {

        int selectedIndex =
                textList.getSelectedIndex();

        if (selectedIndex != -1) {

            textModel.remove(selectedIndex);

            textField.setText("");

            save();
        }
    }

    // --------------------------------------------------
    // SELECT
    // --------------------------------------------------

    private void selectItem() {

        int selectedIndex =
                textList.getSelectedIndex();

        if (selectedIndex != -1) {

            String selectedText =
                    textModel.getElementAt(
                            selectedIndex
                    );

            // Don't put "(done)" into the editable text
            if (selectedText.endsWith(DONE)) {

                selectedText =
                        selectedText.substring(
                                0,
                                selectedText.length()
                                        - DONE.length()
                        );
            }

            textField.setText(selectedText);
        }
    }

    // --------------------------------------------------
    // PRESS ENTER
    // --------------------------------------------------

    @Override
    public void actionPerformed(ActionEvent evt) {

        String text =
                textField.getText().trim();

        if (text.isEmpty()) {
            return;
        }

        // If something is selected -> EDIT
        if (textList.getSelectedIndex() != -1) {

            editItem();

            // Otherwise -> ADD
        } else {

            textModel.addElement(text);

            textField.setText("");

            save();
        }
    }

    // --------------------------------------------------
    // EDIT
    // --------------------------------------------------

    private void editItem() {

        int selectedIndex =
                textList.getSelectedIndex();

        if (selectedIndex != -1) {

            String newText =
                    textField.getText().trim();

            if (!newText.isEmpty()) {

                // Find out whether the old item was done
                String oldText =
                        textModel.getElementAt(
                                selectedIndex
                        );

                boolean wasDone =
                        oldText.endsWith(DONE);

                // Preserve completion status
                if (wasDone) {
                    newText += DONE;
                }

                textModel.setElementAt(
                        newText,
                        selectedIndex
                );

                textList.clearSelection();
                textField.setText("");

                // Save immediately after editing
                save();
            }
        }
    }
}