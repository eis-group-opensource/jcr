/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.util;

import java.io.File;
import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.transaction.TransactionHelper;
import com.exigen.cm.impl.taskmanager.Task;
import com.exigen.cm.impl.taskmanager.TaskManager;

/**
 * The <code>TransientFileFactory</code> utility class can be used to create
 * <i>transient</i> files, i.e. temporary files that are automatically
 * removed once the associated <code>File</code> object is reclaimed by the
 * garbage collector.
 * <p/>
 * File deletion is handled by a low-priority background thread.
 * <p/>
 */
public class TransientFileFactory {

    /**
     * The singleton factory instance
     */
    private static TransientFileFactory INSTANCE;

    /**
     * Queue where <code>MoribundFileReference</code> instances will be enqueued
     * once the associated target <code>File</code> objects have been gc'ed.
     */
    private ReferenceQueue phantomRefQueue = new ReferenceQueue();

    /**
     * Collection of <code>MoribundFileReference</code> instances currently
     * being tracked.
     */
    private Collection trackedRefs = Collections.synchronizedList(new ArrayList());

    /**
     * The reaper thread responsible for removing files awaiting deletion
     */
    private final Task reaper;

    /**
     * Returns the singleton <code>TransientFileFactory</code> instance.
     * @throws RepositoryException 
     */
    public static TransientFileFactory getInstance(){
        synchronized (TransientFileFactory.class) {
            if (INSTANCE == null) {
                try {
					INSTANCE = new TransientFileFactory();
				} catch (RepositoryException e) {
					throw new RuntimeException(e);
				}
            }
            return INSTANCE;
        }
    }

    /**
     * Hidden constructor.
     * @throws RepositoryException 
     */
    private TransientFileFactory() throws RepositoryException {
        // instantiate & start low priority reaper thread
        reaper = new ReaperThread("Transient File Reaper");
        /*reaper.setPriority(Thread.MIN_PRIORITY);
        reaper.setDaemon(true);
        reaper.start();*/
        TaskManager.getInstance(new HashMap<String, String>()).execute(reaper);
        // register shutdownhook for final cleaning up
        
        if (!TransactionHelper.getInstance().isManagementEnvironment()){
	        Runtime.getRuntime().addShutdownHook(new Thread() {
	            public void run() {
	                for (Iterator it = trackedRefs.iterator(); it.hasNext();) {
	                    MoribundFileReference fileRef = (MoribundFileReference) it.next();
	                    fileRef.delete();
	                }
	            }
	        });
        }
    }

    //------------------------------------------------------< factory methods >
    /**
     * Same as {@link File#createTempFile(String, String, File)} except that
     * the newly-created file will be automatically deleted once the
     * returned <code>File</code> object has been gc'ed.
     *
     * @param prefix    The prefix string to be used in generating the file's
     *                  name; must be at least three characters long
     * @param suffix    The suffix string to be used in generating the file's
     *                  name; may be <code>null</code>, in which case the
     *                  suffix <code>".tmp"</code> will be used
     * @param directory The directory in which the file is to be created, or
     *                  <code>null</code> if the default temporary-file
     *                  directory is to be used
     * @return the newly-created empty file
     * @throws IOException If a file could not be created
     */
    public File createTransientFile(String prefix, String suffix, File directory)
            throws IOException {
        File f = File.createTempFile(prefix, suffix, directory);
        trackedRefs.add(new MoribundFileReference(f, phantomRefQueue));
        return f;
    }

    //--------------------------------------------------------< inner classes >
    /**
     * The reaper thread that will remove the files that are ready for deletion.
     */
    private class ReaperThread implements Task {

        ReaperThread(String name) {
        }

        boolean running = true;
        
        /**
         * Run the reaper thread that will delete files as their associated
         * marker objects are reclaimed by the garbage collector.
         */
        public void run() {
            while (running) {
                MoribundFileReference fileRef = null;
                try {
                    // wait until a MoribundFileReference is ready for deletion
                    fileRef = (MoribundFileReference) phantomRefQueue.remove();
                } catch (Exception e) {
                    // silently ignore...
                    continue;
                }
                // delete target
                fileRef.delete();
                fileRef.clear();
                trackedRefs.remove(fileRef);
            }
        }

		public void release() {
			running =false;
		}
    }

    /**
     * Tracker object for a file pending deletion.
     */
    private class MoribundFileReference extends PhantomReference {

        /**
         * The full path to the file being tracked.
         */
        private String path;

        /**
         * Constructs an instance of this class from the supplied parameters.
         *
         * @param file  The file to be tracked.
         * @param queue The queue on to which the tracker will be pushed.
         */
        MoribundFileReference(File file, ReferenceQueue queue) {
            super(file, queue);
            this.path = file.getPath();
        }

        /**
         * Deletes the file associated with this instance.
         *
         * @return <code>true</code> if the file was deleted successfully;
         *         <code>false</code> otherwise.
         */
        boolean delete() {
            return new File(path).delete();
        }
    }
}
