package org.conjugateprior.ca.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GraphicalWordCounter {

	protected File[] getRecursiveDepthOneFileArray(String[] files) throws Exception {
		List<File> filelist = new ArrayList<File>();
		File fail = null;
		for (int ii = 0;  ii < files.length; ii++) {
			File f = new File(files[ii]);
			if (!f.exists()){
				fail = f;
				break;
			} if (f.isDirectory()){
				File[] contents = f.listFiles();
				for (int jj = 0; jj < contents.length; jj++) {
					if (!contents[jj].isDirectory() && !contents[jj].getName().startsWith("."))
						if (contents[jj].length() > 0)
							filelist.add(contents[jj]); // an imperfect filter but...
				}
			} else {
				if (f.length() > 0)
					filelist.add(f);
			}
		}
		if (fail != null)
			throw new Exception("File " + fail.getAbsolutePath() + " does not exist.");
		
		return filelist.toArray(new File[filelist.size()]);
	}
	
	/*
	final JProgressBar progressBar = new JProgressBar(0,100);
    progressBar.setValue(0);
    progressBar.setStringPainted(true);
    
    
	printer.addPropertyChangeListener(new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if ("progress" == evt.getPropertyName()) {
	            int progress = (Integer) evt.getNewValue();
	            progressBar.setValue(progress);
	        } 
		}
	});
	
	JFrame f = new JFrame();
	f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	JPanel p = new JPanel(new BorderLayout());
	p.add(progressBar, BorderLayout.CENTER);
	
	JButton cancel = new JButton("Stop!");
	cancel.addActionListener(new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			boolean b = printer.cancel(false);
			System.err.println("Could we cancel? " + b);
			progressBar.setValue(0);
		}
	});
	p.add(cancel, BorderLayout.EAST);
	
	f.getContentPane().add(p);
	f.pack();
	f.setLocation(200, 200);
	f.setVisible(true);
	*/
	
}
