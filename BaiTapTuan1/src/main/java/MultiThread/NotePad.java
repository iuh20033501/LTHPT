package MultiThread;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.io.FileWriter;

public class NotePad extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	JButton btnOpen;
	JButton btnSave;
	JButton btnExit;
	JButton btnCreate;
	File selectedFile;
	JTextArea textArea;
	private JScrollPane scrollPane;

	public NotePad() {
		setTitle("NotePad");
		setSize(1600, 960);
		setResizable(false);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout(0, 0));
		setLocationRelativeTo(null);
		textArea = new JTextArea();
		getContentPane().add(textArea, BorderLayout.CENTER);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu mnNewMenu = new JMenu("File");
		menuBar.add(mnNewMenu);

		btnCreate = new JButton("Create File");
		mnNewMenu.add(btnCreate);

		btnOpen = new JButton("Open File");
		mnNewMenu.add(btnOpen);

		btnSave = new JButton("Save File");
		mnNewMenu.add(btnSave);

		btnExit = new JButton("Exit");
		mnNewMenu.add(btnExit);
		textArea.setEditable(false);
		
		scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(1585,960));
        getContentPane().add(scrollPane, BorderLayout.EAST); 
		btnExit.addActionListener(this);
		btnOpen.addActionListener(this);
		btnSave.addActionListener(this);
		btnCreate.addActionListener(this);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new NotePad().setVisible(true);
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (o.equals(btnExit)) {
			int confirm = JOptionPane.showConfirmDialog(null, "Bạn có muốn thoát chương trình", "Xác nhận",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (confirm == JOptionPane.YES_OPTION) {
				System.exit(0);
			}
		} else if (o.equals(btnOpen)) {
			JFileChooser fileChooser = new JFileChooser();
			int result = fileChooser.showOpenDialog(this);
			if (result == JFileChooser.APPROVE_OPTION) {
				selectedFile = fileChooser.getSelectedFile();
				ExecutorService executorService = Executors.newFixedThreadPool(4);
				List<Future<String>> fileContentFutures = new ArrayList<>();
				long fileSize = selectedFile.length();
				long partSize = fileSize / 4;
				for (int i = 0; i < 4; i++) {
					long startOffset = i * partSize;
					long endOffset = (i == 3) ? fileSize : (i + 1) * partSize;
					fileContentFutures
							.add(executorService.submit(() -> readFilePart(selectedFile, startOffset, endOffset)));
				}

				SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
					@Override
					protected Void doInBackground() throws Exception {
						for (Future<String> future : fileContentFutures) {
							String fileContent = future.get();
							textArea.append(fileContent);
							textArea.setEditable(true);
							System.out.println(fileContent);
						}
						return null;
					}
				};
				worker.execute();
				executorService.shutdown();

			}
		} else if (o.equals(btnSave)) {
			if (selectedFile != null) {
				int confirm = JOptionPane.showConfirmDialog(null, "Bạn có muốn lưu thay đổi?", "Xác nhận",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (confirm == JOptionPane.YES_OPTION) {
					try (FileWriter fileWriter = new FileWriter(selectedFile)) {
						String contentToSave = textArea.getText();
						fileWriter.write(contentToSave);
						fileWriter.flush();
						JOptionPane.showMessageDialog(this, "Save thành công");
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
				// txtArea.setText("");
			} else {
				JOptionPane.showMessageDialog(this, "Không có file nào để lưu", "Lỗi", JOptionPane.WARNING_MESSAGE);
			}
		} else if (o.equals(btnCreate)) {
			JFileChooser fileChooser = new JFileChooser();
			int result = fileChooser.showSaveDialog(NotePad.this);
			if (result == JFileChooser.APPROVE_OPTION) {
				File newFile = fileChooser.getSelectedFile();
				if (newFile.exists()) {
					int confirm = JOptionPane.showConfirmDialog(null, "Tập tin đã tồn tại");
					if (confirm != JOptionPane.YES_OPTION) {
						return;
					}
				}
				try {
					if (newFile.createNewFile()) {
						JOptionPane.showMessageDialog(null, "Tạo tập tin mới thành công");
						selectedFile = newFile;
						// txtArea.setText("");
					} else {
						JOptionPane.showMessageDialog(null, "Không thể tạo tập tin mới");
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	private String readFilePart(File file, long startOffset, long endOffset) {
		StringBuilder contentPart = new StringBuilder();
		try (FileReader fileReader = new FileReader(file)) {
			char[] buffer = new char[1024];
			int charsRead;
			fileReader.skip(startOffset);
			while (contentPart.length() < endOffset - startOffset && (charsRead = fileReader.read(buffer, 0,
					(int) Math.min(buffer.length, endOffset - startOffset - contentPart.length()))) != -1) {
				contentPart.append(buffer, 0, charsRead);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return contentPart.toString();
	}
}
