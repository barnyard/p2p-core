package com.bt.pi.core.pastry_override;

/*******************************************************************************

 "FreePastry" Peer-to-Peer Application Development Substrate

 Copyright 2002-2007, Rice University. Copyright 2006-2007, Max Planck Institute 
 for Software Systems.  All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are
 met:

 - Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.

 - Neither the name of Rice  University (RICE), Max Planck Institute for Software 
 Systems (MPI-SWS) nor the names of its contributors may be used to endorse or 
 promote products derived from this software without specific prior written 
 permission.

 This software is provided by RICE, MPI-SWS and the contributors on an "as is" 
 basis, without any representations or warranties of any kind, express or implied 
 including, but not limited to, representations or warranties of 
 non-infringement, merchantability or fitness for a particular purpose. In no 
 event shall RICE, MPI-SWS or contributors be liable for any direct, indirect, 
 incidental, special, exemplary, or consequential damages (including, but not 
 limited to, procurement of substitute goods or services; loss of use, data, or 
 profits; or business interruption) however caused and on any theory of 
 liability, whether in contract, strict liability, or tort (including negligence
 or otherwise) arising in any way out of the use of this software, even if 
 advised of the possibility of such damage.

 *******************************************************************************/

/*
 * @(#) PersistenceManager.java
 *
 * @author Ansley Post
 * @author Alan Mislove
 *
 * @version $Id: PersistentStorage.java 4654 2009-01-08 16:33:07Z jeffh $
 */
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import rice.Continuation;
import rice.Continuation.ListenerContinuation;
import rice.environment.Environment;
import rice.environment.logging.Logger;
import rice.environment.processing.WorkRequest;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.IdRange;
import rice.p2p.commonapi.IdSet;
import rice.p2p.util.ImmutableSortedMap;
import rice.p2p.util.RedBlackMap;
import rice.p2p.util.ReverseTreeMap;
import rice.persistence.Storage;

import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.past.content.KoalaGCPastMetadata;

/**
 * This class is an implementation of Storage which provides persistent storage to disk. This class also guarantees that
 * the data will be consistent, even after a crash. This class also provides these services is a non-blocking fashion by
 * launching a separate thread which is tasked with actually writing the data to disk.
 * 
 * This class was initially designed to support only Ids whose toString() method returns a String of constant length. It
 * has been extended to support variable-length toString()s, but we have the caveat that not toString() can be a
 * substring of another toString() - this will cause undefined behavior. <b>Additionally, the toString() method on the
 * key Ids *CANNOT* have the period ('.') or exclamation point ('!') characters in them - these are used for internal
 * purposes.<b>
 * 
 * The serialized objects are stored on-disk in a GZIPed XML format, which provides extensibility with reasonable
 * storage and processing costs. Additionally, any metadata, if provided, is also stored in the on-disk file. The format
 * of the file is
 * 
 * [Key, Object, Version, Gzipped XML] [Metadata, Gzipped XML] [persistence magic number, long] [persistence version,
 * long] [persistence revision, long] [metadata length, long]
 * 
 * The persistence package is set up to automatically upgrade older versions of the on-disk format as new data is
 * written under the key.
 * 
 * Persistence also supports the metadata interface specified in the Catalog interface. All metadata is guaranteed to be
 * stored in memory, so fetching the metadata of a given key is an efficient operation.
 */
@SuppressWarnings("unchecked")
public abstract class PersistentStorage implements Storage {

    /**
     * Fields for logging based on the requests we are writing.
     */
    private Object statLock = new Object();
    private long statsLastWritten;
    private long statsWriteInterval = 60 * 1000;
    private long numWrites = 0;
    private long numReads = 0;
    private long numRenames = 0;
    private long numDeletes = 0;
    private long numMetadataWrites = 0;

    /**
     * Static variables defining the layout of the on-disk storage
     */
    public static final long PERSISTENCE_MAGIC_NUMBER = 8038844221L;
    public static final long PERSISTENCE_VERSION_2 = 2L;
    public static final long PERSISTENCE_REVISION_2_0 = 0L;
    public static final long PERSISTENCE_REVISION_2_1 = 1L;

    /**
     * Static variables which define the location of the storage root
     */
    public static final String BACKUP_DIRECTORY = "/FreePastry-Storage-Root/";
    public static final String LOST_AND_FOUND_DIRECTORY = "lost+found";
    public static final String METADATA_FILENAME = "metadata.cache";

    /**
     * The splitting factor, or the number of files in one directory
     */
    public static final int MAX_FILES = 256;

    /**
     * The maximum number of subdirectories in a directory before splitting
     */
    public static final int MAX_DIRECTORIES = 32;

    /**
     * The amount of time before re-writing the metadata file
     */
    public static final int METADATA_SYNC_TIME = 300000;

    /**
     * Special placeholder for the file whose name should be zero-length
     */
    public static final String ZERO_LENGTH_NAME = "!";

    private KoalaIdFactory factory; // the factory used for creating ids
    private KoalaPiEntityFactory koalaPiEntityFactory;

    private String name; // the name of this instance
    private File rootDirectory; // root directory to store stuff in
    private File backupDirectory; // dir for storing persistent objs
    private File appDirectory; // dir for storing persistent objs
    private File lostDirectory; // dir for lost objects

    private boolean index; // whether or not we are indexing the objects
    private HashMap directories; // the in-memory map of directories (for efficiency)
    private HashMap prefixes; // an in-memory cache of the directory prefixes
    private HashSet dirty; // the list of directories which have dirty metadata

    protected ReverseTreeMap metadata; // the in-memory cache of object metadata

    private String rootDir; // rootDirectory

    private long storageSize; // The amount of storage allowed to be used
    private long usedSize; // The amount of storage currently in use
    private int metaDataSyncTime = METADATA_SYNC_TIME;

    Environment environment;
    Logger logger;

    protected PersistentStorage() {
    }

    // for unit testing
    public void setKoalaIdFactory(KoalaIdFactory koalaIdFactory) {
        this.factory = koalaIdFactory;
    }

    /**
     * Builds a PersistentStorage given a root directory in which to persist the data. Uses a default instance name.
     * 
     * @param factory
     *            The factory to use for creating Ids.
     * @param rootDir
     *            The root directory of the persisted disk.
     * @param size
     *            the size of the storage in bytes, or -1 for unlimited
     */
    public PersistentStorage(KoalaIdFactory factory, String rootDir, long size, Environment env) throws IOException {
        this(factory, "default", rootDir, size, env);
    }

    public void setIndex(boolean i) {
        this.index = i;
    }

    /**
     * Builds a PersistentStorage given and an instance name and a root directory in which to persist the data.
     * 
     * @param factory
     *            The factory to use for creating Ids.
     * @param name
     *            the name of this instance
     * @param rootDir
     *            The root directory of the persisted disk.
     * @param size
     *            the size of the storage in bytes, or -1 for unlimited
     */
    public PersistentStorage(KoalaIdFactory factory, String name, String rootDir, long size, Environment env) throws IOException {
        this(factory, null, name, rootDir, size, true, env, METADATA_SYNC_TIME);
    }

    /**
     * Builds a PersistentStorage given and an instance name and a root directory in which to persist the data.
     * 
     * @param factory
     *            The factory to use for creating Ids.
     * @param name
     *            the name of this instance
     * @param rootDir
     *            The root directory of the persisted disk.
     * @param size
     *            the size of the storage in bytes, or -1 for unlimited
     * @param index
     *            Whether or not to index the objects
     */
    public PersistentStorage(KoalaIdFactory factory, KoalaPiEntityFactory aKoalaPiEntityFactory, String name, String rootDir, long size, boolean index, Environment env, int aMetaDataSyncTime) throws IOException {
        this.environment = env;
        logger = environment.getLogManager().getLogger(PersistentStorage.class, null);
        this.factory = factory;
        this.koalaPiEntityFactory = aKoalaPiEntityFactory;
        this.name = name;
        this.rootDir = rootDir;
        this.storageSize = size;
        this.index = index;
        this.directories = new HashMap();
        this.prefixes = new HashMap();
        statsLastWritten = environment.getTimeSource().currentTimeMillis();

        this.metaDataSyncTime = aMetaDataSyncTime;

        if (index) {
            this.dirty = new HashSet();
            this.metadata = new ReverseTreeMap();
        }

        if (logger.level <= Logger.INFO)
            logger.log("Launching persistent storage in " + rootDir + " with name " + name + " spliting factor " + MAX_FILES);

        init();
    }

    protected KoalaPiEntityFactory getKoalaPiEntityFactory() {
        return koalaPiEntityFactory;
    }

    private void printStats() {
        synchronized (statLock) {
            long now = environment.getTimeSource().currentTimeMillis();
            if ((statsLastWritten / statsWriteInterval) != (now / statsWriteInterval)) {
                if (logger.level <= Logger.INFO)
                    logger.log("@L.PE name=" + name + " interval=" + statsLastWritten + "-" + now);
                statsLastWritten = now;

                if (logger.level <= Logger.INFO)
                    logger.log("@L.PE   objsTotal=" + (index ? "" + metadata.keySet().size() : "?") + " objsBytesTotal=" + getTotalSize());
                if (logger.level <= Logger.INFO)
                    logger.log("@L.PE   numWrites=" + numWrites + " numReads=" + numReads + " numDeletes=" + numDeletes);
                if (logger.level <= Logger.INFO)
                    logger.log("@L.PE   numMetadataWrites=" + numMetadataWrites + " numRenames=" + numRenames);
            }
        }
    }

    protected Id forDht(Id id) {
        return factory.buildId(factory.convertToPId(id).forDht());
    }

    /**
     * Method which allows the persistence root to schedule an event which will tell it to sync the metadata cached.
     * Should be called exactly once after the persistence root is created.
     * 
     * @param timer
     *            The timer to use to schedule the events
     */
    public void setTimer(rice.selector.Timer timer) {
        if (index) {
            timer.scheduleAtFixedRate(new rice.selector.TimerTask() {
                public String toString() {
                    return "persistence dirty purge enqueue";
                }

                public void run() {
                    environment.getProcessor().processBlockingIO(new WorkRequest(new ListenerContinuation("Enqueue of writeMetadataFile", environment), environment.getSelectorManager()) {
                        public String toString() {
                            return "persistence dirty purge";
                        }

                        public Object doWork() throws Exception {
                            writeDirty();
                            return Boolean.TRUE;
                        }
                    });
                }
            }, environment.getRandomSource().nextInt(metaDataSyncTime), metaDataSyncTime);
        }
    }

    /**
     * Renames the given object to the new id. This method is potentially faster than store/cache and unstore/uncache.
     * 
     * @param theOldId
     *            The id of the object in question.
     * @param theNewId
     *            The new id of the object in question.
     * @param c
     *            The command to run once the operation is complete
     */
    public void rename(final Id theOldId, final Id theNewId, Continuation c) {
        final Id oldId = forDht(theOldId);
        final Id newId = forDht(theNewId);
        if (logger.level <= Logger.FINE)
            logger.log("rename " + oldId.toStringFull() + " to " + newId.toStringFull());
        printStats();

        environment.getProcessor().processBlockingIO(new WorkRequest(c, environment.getSelectorManager()) {
            public String toString() {
                return "rename " + oldId + " " + newId;
            }

            public Object doWork() throws Exception {
                synchronized (statLock) {
                    numRenames++;
                }

                File f = getFile(oldId);

                if ((f != null) && (f.exists())) {
                    File g = getFile(newId);
                    renameFile(f, g);

                    checkDirectory(g.getParentFile());

                    if (index) {
                        synchronized (metadata) {
                            metadata.put(newId, metadata.get(oldId));
                            metadata.remove(oldId);
                        }
                    }

                    return Boolean.TRUE;
                } else {
                    return Boolean.FALSE;
                }
            }
        });
    }

    /**
     * Makes the object persistent to disk and stored permanantly
     * 
     * If the object is already persistent, this method will simply update the object's serialized image.
     * 
     * This is implemented atomically so that this may succeed and store the new object, or fail and leave the previous
     * object intact.
     * 
     * This method completes by calling recieveResult() of the provided continuation with the success or failure of the
     * operation.
     * 
     * @param obj
     *            The object to be made persistent.
     * @param theId
     *            The object's id.
     * @param metadata
     *            The object's metadata
     * @param c
     *            The command to run once the operation is complete
     * @return <code>true</code> if the action succeeds, else <code>false</code>.
     */
    public void store(final Id theId, final Serializable metadata, final Serializable obj, Continuation c) {
        if (theId == null || obj == null) {
            c.receiveResult(new Boolean(false));
            return;
        }
        final Id id = forDht(theId);

        printStats();

        environment.getProcessor().processBlockingIO(new WorkRequest(c, environment.getSelectorManager()) {
            public String toString() {
                return "store " + id;
            }

            public Object doWork() throws Exception {
                synchronized (statLock) {
                    numWrites++;
                }
                if (logger.level <= Logger.FINER)
                    logger.log("Storing object " + obj + " under id " + id.toStringFull() + " in root " + appDirectory);

                /* first, create a temporary file */
                File objFile = getFile(id);
                File transcFile = makeTemporaryFile(id);

                if (logger.level <= Logger.FINER)
                    logger.log("Writing object " + obj + " to temporary file " + transcFile + " and renaming to " + objFile);

                /* next, write out the data to a new copy of the original file */
                try {
                    writeObject(obj, metadata, id, environment.getTimeSource().currentTimeMillis(), transcFile);
                    if (logger.level <= Logger.FINER)
                        logger.log("Done writing object " + obj + " under id " + id.toStringFull() + " in root " + appDirectory);

                    /* abort if this will put us over quota */
                    if (getUsedSpace() + getFileLength(transcFile) > getStorageSize())
                        throw new OutofDiskSpaceException();
                } catch (Exception e) {
                    /* if an IOException is thrown, delete the temporary file and abort */
                    if (logger.level <= Logger.WARNING)
                        logger.logException("", e);
                    deleteFile(transcFile);
                    throw e;
                }

                if (logger.level <= Logger.FINER)
                    logger.log("COUNT: Storing data of class " + obj.getClass().getName() + " under " + id.toStringFull() + " of size " + transcFile.length() + " in " + name);

                /* recalculate amount used */
                decreaseUsedSpace(getFileLength(objFile));
                increaseUsedSpace(getFileLength(transcFile));

                /* now, rename the temporary file to be the real file */
                renameFile(transcFile, objFile);

                /* mark the metadata cache of this file to be dirty */
                if (index) {
                    synchronized (PersistentStorage.this.metadata) {
                        PersistentStorage.this.metadata.put(id, metadata);
                        dirty.add(objFile.getParentFile());
                    }
                }

                /* finally, check to see if this directory needs to be split */
                checkDirectory(objFile.getParentFile());

                return Boolean.TRUE;
            }
        });
    }

    /**
     * Request to remove the object from the list of persistend objects. Delete the serialized image of the object from
     * stable storage. If necessary. If the object was not in the cached list in the first place, nothing happens and
     * <code>false</code> is returned.
     * 
     * This method also guarantees that the data on disk will remain consistent, even after a crash by performing the
     * delete atomically.
     * 
     * This method completes by calling recieveResult() of the provided continuation with the success or failure of the
     * operation.
     * 
     * @param theId
     *            The object's persistence id
     * @param c
     *            The command to run once the operation is complete
     * @return <code>true</code> if the action succeeds, else <code>false</code>.
     */
    public void unstore(final Id theId, Continuation c) {
        final Id id = forDht(theId);
        printStats();

        environment.getProcessor().processBlockingIO(new WorkRequest(c, environment.getSelectorManager()) {
            public String toString() {
                return "unstore " + id;
            }

            public Object doWork() throws Exception {
                synchronized (statLock) {
                    numDeletes++;
                }

                /* first get the file */
                File objFile = getFile(id);

                if (logger.level <= Logger.FINE)
                    logger.log("COUNT: Unstoring data under " + id.toStringFull() + " of size " + objFile.length() + " in " + name + " from " + objFile);

                /* remove id from stored list */
                if (index) {
                    synchronized (metadata) {
                        metadata.remove(id);
                        dirty.add(objFile.getParentFile());
                    }
                }

                /* check to make sure file exists */
                if ((objFile == null) || (!objFile.exists()))
                    return Boolean.FALSE;

                /* record the space collected and delete the file */
                decreaseUsedSpace(objFile.length());
                deleteFile(objFile);

                return Boolean.TRUE;
            }
        });
    }

    /**
     * Returns whether or not an object is present in the location <code>id</code>.
     * 
     * @param id
     *            The id of the object in question.
     * @return Whether or not an object is present at id.
     */
    public boolean exists(Id id) {
        if (index) {
            synchronized (metadata) {
                return metadata.containsKey(forDht(id));
            }
        } else {
            throw new UnsupportedOperationException("exists() not supported without indexing");
        }
    }

    /**
     * Returns the metadata associated with the provided object, or null if no metadata exists. The metadata must be
     * stored in memory, so this operation is guaranteed to be fast and non-blocking.
     * 
     * @param id
     *            The id for which the metadata is needed
     * @return The metadata, or null of non exists
     */
    public Serializable getMetadata(Id id) {
        if (index) {
            synchronized (metadata) {
                return (Serializable) metadata.get(forDht(id));
            }
        } else {
            throw new UnsupportedOperationException("getMetadata() not supported without indexing");
        }
    }

    /**
     * Updates the metadata stored under the given key to be the provided value. As this may require a disk access, the
     * requestor must also provide a continuation to return the result to.
     * 
     * @param theId
     *            The id for the metadata
     * @param metadata
     *            The metadata to store
     * @param c
     *            The command to run once the operation is complete
     */
    public void setMetadata(final Id theId, final Serializable metadata, Continuation c) {
        final Id id = forDht(theId);
        printStats();

        if (!exists(id)) {
            c.receiveResult(new Boolean(false));
        } else {
            environment.getProcessor().processBlockingIO(new WorkRequest(c, environment.getSelectorManager()) {
                public String toString() {
                    return "setMetadata " + id;
                }

                public Object doWork() throws Exception {
                    synchronized (statLock) {
                        numMetadataWrites++;
                    }

                    if (logger.level <= Logger.FINER)
                        logger.log("COUNT: Updating metadata for " + id.toStringFull() + " in " + name);

                    /* write the metadata to the file */
                    File objFile = getFile(id);
                    writeMetadata(objFile, metadata);

                    /* then update our cache */
                    if (index) {
                        synchronized (PersistentStorage.this.metadata) {
                            PersistentStorage.this.metadata.put(id, metadata);
                            dirty.add(objFile.getParentFile());
                        }
                    }

                    return Boolean.TRUE;
                }
            });
        }
    }

    /**
     * Returns the object identified by the given id.
     * 
     * @param theId
     *            The id of the object in question.
     * @param c
     *            The command to run once the operation is complete
     * @return The object, or <code>null</code> if there is no corresponding object (through receiveResult on c).
     */
    public void getObject(final Id theId, Continuation c) {
        final Id id = forDht(theId);
        if (logger.level <= Logger.FINE)
            logger.log("getObject " + id.toStringFull());
        printStats();

        if (index && (!exists(id))) {
            if (logger.level <= Logger.FINE)
                logger.log("id " + id.toStringFull() + " not in index");
            c.receiveResult(null);
        } else {
            if (logger.level <= Logger.FINE)
                logger.log("selectorManager " + environment.getSelectorManager());
            environment.getProcessor().processBlockingIO(new WorkRequest(c, environment.getSelectorManager()) {
                public String toString() {
                    return "getObject " + id;
                }

                public Object doWork() throws Exception {
                    if (logger.level <= Logger.FINE)
                        logger.log("doWork()");
                    synchronized (statLock) {
                        numReads++;
                    }

                    /* get the file */
                    File objFile = getFile(id);
                    if (logger.level <= Logger.FINE)
                        logger.log("objFile " + objFile + " " + (null == objFile ? "false" : objFile.exists()));

                    try {
                        /* and make sure that it exists */
                        if ((objFile == null) || (!objFile.exists()))
                            return null;

                        if (logger.level <= Logger.FINE)
                            logger.log("COUNT: Fetching data under " + id.toStringFull() + " of size " + objFile.length() + " in " + name);
                        return readData(objFile);
                    } catch (Exception e) {
                        logger.logException("error in getObject", e);
                        /* remove our index for this file */
                        if (index) {
                            synchronized (metadata) {
                                metadata.remove(id);
                                dirty.add(objFile.getParentFile());
                            }
                        }

                        /* if there's a problem, move the file to the lost+found */
                        moveToLost(objFile);

                        throw e;
                    }
                }
            });
        }
    }

    /**
     * Return the objects identified by the given range of ids. The IdSet returned contains the Ids of the stored
     * objects. The range is partially inclusive, the lower range is inclusive, and the upper exclusive.
     * 
     * 
     * NOTE: This method blocks so if the behavior of this method changes and uses the disk, this method may be
     * deprecated.
     * 
     * @param range
     *            The range to query
     * @return The idset containing the keys
     */
    public IdSet scan(IdRange range) {
        // TODO: make the incoming range use forDht() ?
        if (index) {
            if (range.isEmpty())
                return factory.buildIdSet();
            else if (range.getCCWId().equals(range.getCWId()))
                return scan();
            else
                synchronized (metadata) {
                    SortedMap keySubMap = metadata.keySubMap(range.getCCWId(), range.getCWId());
                    SortedMap cleanKeySubMap = removeDeleted(keySubMap);
                    return factory.buildIdSet(new ImmutableSortedMap(cleanKeySubMap));
                }
        } else {
            throw new UnsupportedOperationException("scan() not supported without indexing");
        }
    }

    private SortedMap removeDeleted(SortedMap input) {
        SortedMap result = new TreeMap();
        for (Object o : input.keySet()) {
            Id id = (Id) o;
            KoalaGCPastMetadata gcPastMetadata = (KoalaGCPastMetadata) metadata.get(id);
            if (null != gcPastMetadata && (gcPastMetadata.isDeletedAndDeletable() || null == gcPastMetadata.getEntityType()))
                continue;
            result.put(id, input.get(id));
        }
        return result;
    }

    /**
     * Return the objects identified by the given range of ids. The IdSet returned contains the Ids of the stored
     * objects. The range is partially inclusive, the lower range is inclusive, and the upper exclusive.
     * 
     * NOTE: This method blocks so if the behavior of this method changes and uses the disk, this method may be
     * deprecated.
     * 
     * @return The idset containing the keys
     */
    public IdSet scan() {
        if (index) {
            synchronized (metadata) {
                SortedMap keyMap = metadata.keyMap();
                SortedMap cleanKeyMap = removeDeleted(keyMap);
                return factory.buildIdSet(new ImmutableSortedMap(cleanKeyMap));
            }
        } else {
            throw new UnsupportedOperationException("scan() not supported without indexing");
        }
    }

    /**
     * Returns a map which contains keys mapping ids to the associated metadata.
     * 
     * @param range
     *            The range to query
     * @return The map containing the keys
     */
    public SortedMap scanMetadata(IdRange range) {
        // TODO: make the incoming range use forDht() ?
        if (index) {
            if (range.isEmpty())
                return new RedBlackMap();
            else if (range.getCCWId().equals(range.getCWId()))
                return scanMetadata();
            else
                synchronized (metadata) {
                    return new ImmutableSortedMap(metadata.keySubMap(range.getCCWId(), range.getCWId()));
                }
        } else {
            throw new UnsupportedOperationException("scanMetadata() not supported without indexing");
        }
    }

    /**
     * Returns a map which contains keys mapping ids to the associated metadata.
     * 
     * @return The treemap mapping ids to metadata
     */
    public SortedMap scanMetadata() {
        if (index) {
            return new ImmutableSortedMap(metadata.keyMap());
        } else {
            throw new UnsupportedOperationException("scanMetadata() not supported without indexing");
        }
    }

    /**
     * Returns the submapping of ids which have metadata less than the provided value.
     * 
     * @param value
     *            The maximal metadata value
     * @return The submapping
     */
    public SortedMap scanMetadataValuesHead(Object value) {
        if (index) {
            return new ImmutableSortedMap(metadata.valueHeadMap(value));
        } else {
            throw new UnsupportedOperationException("scanMetadataValuesHead() not supported without indexing");
        }
    }

    /**
     * Returns the submapping of ids which have metadata null
     * 
     * @return The submapping
     */
    public SortedMap scanMetadataValuesNull() {
        if (index) {
            return new ImmutableSortedMap(metadata.valueNullMap());
        } else {
            throw new UnsupportedOperationException("scanMetadataValuesNull() not supported without indexing");
        }
    }

    /**
     * Returns the total size of the stored data in bytes.The result is returned via the receiveResult method on the
     * provided Continuation with an Integer representing the size.
     * 
     * @param c
     *            The command to run once the operation is complete
     * @return The total size, in bytes, of data stored.
     */
    public long getTotalSize() {
        return usedSize;
    }

    /**
     * Returns the number of Ids currently stored in the catalog
     * 
     * @return The number of ids in the catalog
     */
    public int getSize() {
        if (index) {
            return metadata.size();
        } else {
            throw new UnsupportedOperationException("getSize() not supported without indexing");
        }
    }

    /**
     * Method which is used to erase all data stored in the Catalog. Use this method with care!
     * 
     * @param c
     *            The command to run once done
     */
    public void flush(Continuation c) {
        environment.getProcessor().processBlockingIO(new WorkRequest(c, environment.getSelectorManager()) {
            public String toString() {
                return "flush";
            }

            public Object doWork() throws Exception {
                if (logger.level <= Logger.FINER)
                    logger.log("COUNT: Flushing all data in " + name);

                flushDirectory(appDirectory);
                return Boolean.TRUE;
            }
        });
    }

    /*****************************************************************/
    /* Functions for init/crash recovery                             */
    /*****************************************************************/

    /**
     * Perform all the miscellaneous house keeping that must be done when we start up
     */
    private void init() throws IOException {
        if (logger.level <= Logger.INFO)
            logger.log("Initing directories");
        initDirectories();
        if (logger.level <= Logger.INFO)
            logger.log("Initing directory map");
        initDirectoryMap(appDirectory);
        if (logger.level <= Logger.INFO)
            logger.log("Initing files");
        initFiles(appDirectory);
        if (logger.level <= Logger.INFO)
            logger.log("Initing file map");
        initFileMap(appDirectory);
        if (logger.level <= Logger.INFO)
            logger.log("Syncing metadata");
        if (index)
            writeDirty();
        if (logger.level <= Logger.INFO)
            logger.log("Done initing");

        // get metadata to persisted on a regular basis
        setTimer(environment.getSelectorManager().getTimer());
    }

    /**
     * Verify that the directory name passed to the PersistenceManagerImpl constructor is valid and creates the
     * necessary subdirectories.
     * 
     * @return Whether the directories are successfully initialized.
     */
    private void initDirectories() throws IOException {
        rootDirectory = new File(rootDir);
        createDirectory(rootDirectory);

        backupDirectory = new File(rootDirectory, BACKUP_DIRECTORY);
        createDirectory(backupDirectory);

        appDirectory = new File(backupDirectory, getName());
        createDirectory(appDirectory);

        lostDirectory = new File(backupDirectory, LOST_AND_FOUND_DIRECTORY);
        createDirectory(lostDirectory);
    }

    /**
     * Reads in the in-memory map of directories for use.
     * 
     * @param dir
     *            The directory to recurse
     */
    private void initDirectoryMap(File dir) {
        File[] files = dir.listFiles(new DirectoryFilter());
        directories.put(dir, files);

        for (int i = 0; i < files.length; i++)
            initDirectoryMap(files[i]);
    }

    /**
     * Ensures that all files are in the correct directories, and moves files which are in the wrong directory. Also
     * checks for any temporary files and resolves any detected conflicts.
     * 
     * @param dir
     *            The directory to start on
     */
    private void initFiles(File dir) throws IOException {
        String[] dirs = dir.list(new DirectoryFilter());
        String[] files = dir.list(new FileFilter());

        /* first, init any files, and relocate any files if there are dirs */
        for (int i = 0; i < files.length; i++) {
            try {
                if (!initTemporaryFile(dir, files[i])) {

                    /* if there are directories in the dir, then move the file */
                    if (dirs.length > 0)
                        moveFileToCorrectDirectory(dir, files[i]);
                }
            } catch (Exception e) {
                if (logger.level <= Logger.WARNING)
                    logger.logException("Got exception " + e + " initting file " + files[i] + " - moving to lost+found.", e);
                moveToLost(new File(dir, files[i]));
            }
        }

        /* next, recurse into any dirs */
        for (int i = 0; i < dirs.length; i++)
            initFiles(new File(dir, dirs[i]));

        /* and delete the old metadata file, if it exists */
        if (dirs.length > 0)
            deleteFile(new File(dir, METADATA_FILENAME));
    }

    /**
     * Method which initializes a temporary file by doing the following: 1. If this file is not a temporary file, simply
     * returns the file 2. If so, it simply moves this file to the lost and found and returns null
     * 
     * This is done since all temporary files are, by definition, temporary until they are renamed to an actual ID name.
     * Thus, if we detect a temporary file during start-up, it is likely that the file is corrupted and should be
     * deleted.
     * 
     * @returns Whether or not the file was a temporary file
     */
    private boolean initTemporaryFile(File parent, String name) throws IOException {
        if (!isTemporaryFile(name))
            return false;

        moveToLost(new File(parent, name));
        return true;
    }

    /**
     * Inititializes the idSet data structure
     * 
     * In doing this it must resolve conflicts and aborted transactions. After this is run the most current stable state
     * should be restored. Also record the total used space for all files in the root. Lastly, deletes any files which
     * are of zero length.
     * 
     */
    private void initFileMap(File dir) throws IOException {
        if (logger.level <= Logger.FINE)
            logger.log("Initting directory " + dir);

        /* first, see if this directory needs to be expanded */
        checkDirectory(dir);

        /* make sure the directory was not pruned */
        if (!dir.exists())
            return;

        /* now, read the metadata file in this directory */
        long modified = 0;

        if (index) {
            try {
                modified = readMetadataFile(dir);
            } catch (IOException e) {
                if (logger.level <= Logger.SEVERE)
                    logger.logException("Got exception " + e + " reading metadata file - regenerating", e);
            }
        }

        /* next, start processing by listing the number of files and going from there */
        File[] files = dir.listFiles(new FileFilter());
        if (logger.level <= Logger.FINE)
            logger.log("files: " + Arrays.asList(files));
        File[] dirs = dir.listFiles(new DirectoryFilter());
        if (logger.level <= Logger.FINE)
            logger.log("dirs: " + Arrays.asList(dirs));

        for (int i = 0; i < files.length; i++) {
            try {
                Id id = readKey(files[i]);
                long len = getFileLength(files[i]);

                if (id == null)
                    if (logger.level <= Logger.INFO)
                        logger.log("READING " + files[i] + " RETURNED NULL!");

                if (len > 0) {
                    increaseUsedSpace(len);

                    /* if the file is newer than the metadata file, update the metadata 
                    if we don't have the metadata for this file, update it */
                    if (index && ((!metadata.containsKey(id)) || (files[i].lastModified() > modified))) {
                        if (logger.level <= Logger.FINER)
                            logger.log("Reading newer metadata out of file " + files[i] + " id " + id.toStringFull() + " " + files[i].lastModified() + " " + modified + " " + metadata.containsKey(id));
                        metadata.put(id, readMetadata(files[i]));
                        dirty.add(dir);
                    }
                } else {
                    moveToLost(files[i]);

                    if (index && metadata.containsKey(id)) {
                        metadata.remove(id);
                        dirty.add(dir);
                    }
                }
            } catch (Exception e) {
                if (logger.level <= Logger.WARNING)
                    logger.logException("ERROR: Received Exception " + e + " while initing file " + files[i] + " - moving to lost+found.", e);
                moveToLost(files[i]);
            }
        }

        /* now recurse and check all of the children */
        for (int i = 0; i < dirs.length; i++)
            initFileMap(dirs[i]);

        /* and finally see if this directory needs to be pruned or expanded */
        checkDirectory(dir);
    }

    /**
     * Resolves a conflict between the two provided files by picking the newer one and renames it to the given output
     * file.
     * 
     * @param file1
     *            The first file
     * @param file2
     *            The second file
     * @param output
     *            The file to store the result in
     */
    private void resolveConflict(File file1, File file2, File output) throws IOException {
        if (!file2.exists()) {
            renameFile(file1, output);
        } else if (!file1.exists()) {
            renameFile(file2, output);
        } else if (file1.equals(file2)) {
            renameFile(file1, output);
        } else {
            if (logger.level <= Logger.FINE)
                logger.log("resolving conflict between " + file1 + " and " + file2);

            if (readVersion(file1) < readVersion(file2)) {
                moveToLost(file1);
                renameFile(file2, output);
            } else {
                moveToLost(file2);
                renameFile(file1, output);
            }
        }
    }

    /**
     * Moves a file to the lost and found directory for this instance
     * 
     * @param file
     *            the file to be moved
     * 
     */
    private void moveToLost(File file) throws IOException {
        renameFile(file, new File(lostDirectory, getPrefix(file.getParentFile()) + file.getName()));
    }

    /*****************************************************************/
    /* Helper functions for Directory Splitting Management           */
    /*****************************************************************/

    /**
     * Method which checks to see if a directory has too many files, and if so, expands the directory in order to bring
     * it to the correct size
     * 
     * @param dir
     *            The directory to check
     * @return Whether or not the directory was modified
     */
    private boolean checkDirectory(File directory) throws IOException {
        int files = numFilesDir(directory);
        int dirs = numDirectoriesDir(directory);

        if (logger.level <= Logger.FINE)
            logger.log("Checking directory " + directory + " for oversize " + files + "/" + dirs);

        if (files > MAX_FILES) {
            expandDirectory(directory);
            return true;
        } else if (dirs > MAX_DIRECTORIES) {
            reformatDirectory(directory);
            return true;
        } else if ((files == 0) && (dirs == 0) && (!directory.equals(appDirectory))) {
            pruneDirectory(directory);
            return true;
        }

        return false;
    }

    /**
     * This method removes an empty directory from the storage root, and updates all of the associated metadata.
     * 
     * @param dir
     *            The directory to remove
     */
    private void pruneDirectory(File dir) throws IOException {
        if (logger.level <= Logger.FINE)
            logger.log("Pruning directory " + dir + " due to emptiness");

        /* First delete the metadata file, if it exists */
        deleteFile(new File(dir, METADATA_FILENAME));

        /* Then remove the directory */
        deleteDirectory(dir);

        /* Finally update the metadata */
        directories.remove(dir);
        prefixes.remove(dir);
        this.directories.put(dir.getParentFile(), dir.getParentFile().listFiles(new DirectoryFilter()));
    }

    /**
     * This method expands the directory when there are too many subdirectories in one directory. Basically uses the
     * same logic as expandDirectory(), but also updates the metadata.
     * 
     * This is used to keep less then NUM_FILES in any directory at one time. This speeds up look up time in a
     * particular directory under certain circumstances.
     * 
     * @param dir
     *            The directory to expand
     */
    private void reformatDirectory(File dir) throws IOException {
        if (logger.level <= Logger.FINE)
            logger.log("Expanding directory " + dir + " due to too many subdirectories");
        /* first, determine what directories we should create (ignoring the ! directories) */
        String[] newDirNames = getDirectories(dir.list(new DirectoryFilter()));
        reformatDirectory(dir, newDirNames);
        if (logger.level <= Logger.FINE)
            logger.log("Done expanding directory " + dir);
    }

    /**
     * This method performs a directory expansion, given the names of the subdirectories to create.
     * 
     * @param dir
     *            The directory to expand
     * @param newDirNames
     *            The array containing the names of the new directories
     */
    private void reformatDirectory(File dir, String[] newDirNames) throws IOException {
        String[] dirNames = dir.list(new DirectoryFilter());
        File[] newDirs = new File[newDirNames.length];

        /* create the new directories, move the old ones */
        for (int i = 0; i < newDirNames.length; i++) {
            newDirs[i] = new File(dir, newDirNames[i]);
            createDirectory(newDirs[i]);
            if (logger.level <= Logger.FINE)
                logger.log("Creating directory " + newDirNames[i]);

            /* now look through the original directory and move any matching dirs */
            String[] subDirNames = getMatchingDirectories(newDirNames[i], dirNames);
            File[] newSubDirs = new File[subDirNames.length];

            for (int j = 0; j < subDirNames.length; j++) {
                /* move the directory */
                File oldDir = new File(dir, subDirNames[j]);
                newSubDirs[j] = new File(newDirs[i], subDirNames[j].substring(newDirNames[i].length()));
                if (logger.level <= Logger.FINE)
                    logger.log("Moving the old direcotry " + oldDir + " to " + newSubDirs[j]);
                renameFile(oldDir, newSubDirs[j]);

                /* remove the stale entry, add the new one */
                this.directories.remove(oldDir);
                this.directories.put(newSubDirs[j], new File[0]);
            }

            this.directories.put(newDirs[i], newSubDirs);
        }

        /* lastly, update the root directory */
        this.directories.put(dir, newDirs);
    }

    /**
     * Returns the sublist of the provided list which starts with the given prefix.
     * 
     * @param prefix
     *            The prefix to look for
     * @param list
     *            The list to search through
     * @return The sublist
     */
    private String[] getMatchingDirectories(String prefix, String[] dirNames) {
        Vector result = new Vector();

        for (int i = 0; i < dirNames.length; i++)
            if (dirNames[i].startsWith(prefix))
                result.add(dirNames[i]);

        return (String[]) result.toArray(new String[0]);
    }

    /**
     * This method expands the directory in to a subdirectory for each prefix, contained in the directory
     * 
     * This is used to keep less then NUM_FILES in any directory at one time. This speeds up look up time in a
     * particular directory under certain circumstances.
     * 
     * @param dir
     *            The directory to expand
     */
    private void expandDirectory(File dir) throws IOException {
        if (logger.level <= Logger.FINE)
            logger.log("Expanding directory " + dir + " due to too many files");
        /* first, determine what directories we should create */
        String[] fileNames = dir.list(new FileFilter());
        String[] dirNames = getDirectories(fileNames);
        File[] dirs = new File[dirNames.length];

        /* create the directories */
        for (int i = 0; i < dirNames.length; i++) {
            dirs[i] = new File(dir, dirNames[i]);
            directories.put(dirs[i], new File[0]);

            if (dirs[i].exists() && dirs[i].isFile())
                renameFile(dirs[i], new File(dir, dirs[i].getName() + ZERO_LENGTH_NAME));

            createDirectory(dirs[i]);
            if (logger.level <= Logger.FINE)
                logger.log("Creating directory " + dirNames[i]);

            /* mark this directory for metadata syncing */
            if (index)
                dirty.add(dirs[i]);
        }

        /* add the list of directories to the map */
        directories.put(dir, dirs);

        /* last, move the files into the correct directory */
        File[] files = dir.listFiles(new FileFilter());
        for (int i = 0; i < files.length; i++) {
            for (int j = 0; j < dirs.length; j++) {
                if (files[i].getName().startsWith(dirs[j].getName())) {
                    if (logger.level <= Logger.FINEST)
                        logger.log("Renaming file " + files[i] + " to " + new File(dirs[j], files[i].getName().substring(dirs[j].getName().length())));
                    renameFile(files[i], new File(dirs[j], files[i].getName().substring(dirs[j].getName().length())));
                    break;
                }
            }
        }

        /* and remove the metadata file */
        deleteFile(new File(dir, METADATA_FILENAME));

        if (logger.level <= Logger.FINE)
            logger.log("Done expanding directory " + dir);
    }

    /**
     * This method takes in a list of the file names in a given directory which needs to be expanded, and returns the
     * lists of directories which should be created for the expansion to happen.
     * 
     * @param names
     *            The names to return the directories for
     * @return The directory names
     */
    private String[] getDirectories(String[] names) {
        int length = getPrefixLength(names);
        String prefix = names[0].substring(0, length);
        CharacterHashSet set = new CharacterHashSet();

        for (int i = 0; i < names.length; i++) {
            if (names[i].length() > length)
                set.put(names[i].charAt(length));
        }

        char[] splits = set.get();
        String[] result = new String[splits.length];

        for (int i = 0; i < result.length; i++)
            result[i] = prefix + splits[i];

        return result;
    }

    /**
     * This method takes in the list of file names in a given directory and returns the longest common prefix of all of
     * the names.
     * 
     * @param names
     *            The names to find the prefix of
     * @return The longest common prefix of all of the names
     */
    private int getPrefixLength(String[] names) {
        int length = names[0].length() - 1;

        for (int i = 0; i < names.length; i++)
            length = getPrefixLength(names[0], names[i], length);

        return length;
    }

    /**
     * Method which takes in two strings and returns the length of the shared prefix, or a predefined maximum.
     * 
     * @param a
     *            The first string
     * @param b
     *            The second string
     * @param max
     *            The maximum value to return
     */
    private int getPrefixLength(String a, String b, int max) {
        int i = 0;

        for (; (i < a.length() - 1) && (i < b.length() - 1) && (i < max); i++)
            if (a.charAt(i) != b.charAt(i))
                return i;

        return i;
    }

    /**
     * Takes a file and moves it to the correct directory this is used in cojunction with expand to move files to their
     * correct subdirectories below the expanded directory
     * 
     * @param file
     *            The file to be moved
     */
    private void moveFileToCorrectDirectory(File parent, String name) throws IOException {
        File file = new File(parent, name);
        Id id = readKeyFromFile(file);
        File dest = getDirectoryForId(id);

        /* if it's in the wrong directory, then move it and resolve the conflict if necessary */
        if (!dest.equals(parent)) {
            if (logger.level <= Logger.FINE)
                logger.log("moving file " + file + " to correct directory " + dest + " from " + parent);
            File other = new File(dest, id.toStringFull().substring(getPrefix(dest).length()));
            resolveConflict(file, other, other);
            checkDirectory(dest);
        }
    }

    /**
     * Method which recursively flushes a directory hierarchy, removing all data. Be careful!
     * 
     * @param dir
     *            The directory to flush
     */
    private void flushDirectory(File dir) throws IOException {
        logger.log("Flushing file " + dir);

        if (!dir.isDirectory()) {
            Id id = readKey(dir);

            /* remove id from stored list */
            if (index) {
                synchronized (metadata) {
                    metadata.remove(id);
                }
            }

            /* record the space collected and delete the file */
            decreaseUsedSpace(dir.length());
            deleteFile(dir);
        } else {
            File[] dirs = dir.listFiles();

            /* remove all subdirectories */
            for (int i = 0; i < dirs.length; i++) {
                flushDirectory(dirs[i]);

                /* update the metadata */
                directories.remove(dirs[i]);
                prefixes.remove(dirs[i]);

                /* delete the dir */
                deleteFile(dirs[i]);
            }
        }
    }

    /*****************************************************************/
    /* Helper functions for File Management                          */
    /*****************************************************************/

    /**
     * Create a directory given its name
     * 
     * @param directory
     *            The directory to be created
     * @return Whether the directory is successfully created.
     */
    private static void createDirectory(File directory) throws IOException {
        if ((directory == null) || (directory.exists() && directory.isFile()) || (!(directory.exists()) && (!directory.mkdirs())))
            throw new IOException("Creation of directory " + directory + " failed!");
    }

    /**
     * Removes an empty directory given its name
     * 
     * @param directory
     *            The directory to be created
     * @return Whether the directory is successfully created.
     */
    private static void deleteDirectory(File directory) throws IOException {
        if ((directory != null) && directory.exists()) {
            if (directory.listFiles().length > 0)
                throw new IOException("Cannot delete " + directory + " - directory is not empty!");

            if (!directory.delete())
                throw new IOException("Deletion of directory " + directory + " failed!");
        }
    }

    /**
     * 
     * Returns the length of a file in bytes
     * 
     * @param file
     *            file whose length to get
     */
    private static long getFileLength(File file) {
        return (((file != null) && file.exists()) ? file.length() : 0);
    }

    /**
     * Renames a given file on disk.
     * 
     * @param oldFile
     *            The old name
     * @param newFile
     *            The new name
     */
    private static void renameFile(File oldFile, File newFile) throws IOException {
        if ((oldFile != null) && oldFile.exists() && (!oldFile.equals(newFile))) {
            deleteFile(newFile);

            if (!oldFile.renameTo(newFile))
                throw new IOException("Rename of " + oldFile + " to " + newFile + " failed!");
        }
    }

    /**
     * deletes a given file from disk
     * 
     * @param file
     *            file to be deleted.
     * 
     */
    private static void deleteFile(File file) throws IOException {
        if ((file != null) && file.exists() && (!file.delete()))
            throw new IOException("Delete of " + file + " failed!");
    }

    /**
     * Returns whether or not the given file is a temporary file.
     * 
     * @param name
     *            The name of the file
     * @return Whether or not it is temporary
     */
    private boolean isTemporaryFile(String name) {
        return (name.indexOf(".") >= 0);
    }

    /**
     * Generates a new file name to assign for a given id
     * 
     * @param Comparable
     *            id the id to generate a name for
     * @return String the new File name
     * 
     *         This method will return the hashcode of the object used as the id unless there is a collision, in which
     *         case it will return a random number Since this mapping is only needed once it does not matter what number
     *         is used to generate the filename, the hashcode is the first try for efficiency.
     */
    private File makeTemporaryFile(Id id) throws IOException {
        File directory = getDirectoryForId(id);
        File file = new File(directory, id.toStringFull().substring(getPrefix(directory).length()) + "." + environment.getRandomSource().nextInt() % 100);

        while (file.exists())
            file = new File(directory, id.toStringFull().substring(getPrefix(directory).length()) + "." + environment.getRandomSource().nextInt() % 100);

        return file;
    }

    /**
     * Gets the file for a certain id from the disk
     * 
     * @param id
     *            the id to get a file for
     * @return File the file for the id
     */
    private File getFile(Id id) throws IOException {
        logger.log(String.format("getFile(%s)", id.toStringFull()));
        File dir = getDirectoryForId(id);
        String name = id.toStringFull().substring(getPrefix(dir).length());
        if (name.equals(""))
            name = ZERO_LENGTH_NAME;

        // check for ! directory
        File file = new File(dir, name);
        if (file.exists() && file.isDirectory())
            file = new File(file, ZERO_LENGTH_NAME);

        return file;
    }

    /**
     * Gets the directory an id should be stored in
     * 
     * @param id
     *            the string representation of the id
     * @return File the directory that should contain an id
     */
    private File getDirectoryForId(Id id) throws IOException {
        return getDirectoryForName(id.toStringFull());
    }

    /**
     * Gets the directory a given file should be stored in
     * 
     * @param name
     *            The name of the file
     * @return File the directory that should contain an id
     */
    private File getDirectoryForName(String name) throws IOException {
        return getDirectoryForName(name, appDirectory);
    }

    /**
     * Gets the best directory for the given name
     * 
     * @param name
     *            The name to serach for
     * @param dir
     *            The directory to start at
     * @return The directory the name should be stored in
     */
    private File getDirectoryForName(String name, File dir) throws IOException {
        logger.log(String.format("getDirectoryForName(%s, %s)", name, dir));
        File[] subDirs = (File[]) directories.get(dir);

        if (subDirs.length == 0) {
            return dir;
        } else {
            for (int i = 0; i < subDirs.length; i++)
                if (name.startsWith(subDirs[i].getName()))
                    return getDirectoryForName(name.substring(subDirs[i].getName().length()), subDirs[i]);
                else if ((name.length() == 0) && subDirs[i].getName().equals(ZERO_LENGTH_NAME))
                    return getDirectoryForName(name, subDirs[i]);

            /* here, we must create the appropriate directory */
            if ((name.length() >= subDirs[0].getName().length()) || ((name.length() == 0) && (subDirs[0].getName().length() == 1))) {
                File newDir = new File(dir, (name.length() == 0 ? ZERO_LENGTH_NAME : name.substring(0, subDirs[0].getName().length())));
                if (logger.level <= Logger.FINE)
                    logger.log("Necessarily creating dir " + newDir.getName());
                createDirectory(newDir);
                this.directories.put(dir, append(subDirs, newDir));
                this.directories.put(newDir, new File[0]);

                /* finally, we must check if this caused too many dirs in one dir.  If so, we
                   simply rerun the algorithm which will reflect the new dir */
                if (checkDirectory(dir)) {
                    return getDirectoryForName(name, dir);
                } else {
                    return newDir;
                }
            } else {
                /* here, we have to handle a weird case where the filename is less than that of
                an existing directory.  To handle this, we pretend like we're doing a directory
                split, and create new subdirs so we can accomidate everything. */
                String[] dirs = new String[subDirs.length + 1];

                for (int i = 0; i < subDirs.length; i++)
                    dirs[i] = subDirs[i].getName();
                dirs[subDirs.length] = (name.length() == 0 ? ZERO_LENGTH_NAME : name);

                /* now reformat the directory, creating an entry for this name */
                reformatDirectory(dir, getDirectories(dirs));

                /* and finally, recurse, which should find a directory for this name */
                return getDirectoryForName(name, dir);
            }
        }
    }

    /**
     * Internal method which returns the prefix of a given directory
     * 
     * @param file
     *            The directory to return the prefix for
     * @return The prefix
     */
    private String getPrefix(File file) {
        if (prefixes.get(file) != null)
            return (String) prefixes.get(file);

        StringBuffer buffer = new StringBuffer();
        while (!file.equals(appDirectory)) {
            buffer.insert(0, file.getName().replaceAll(ZERO_LENGTH_NAME, ""));
            file = file.getParentFile();
        }
        prefixes.put(file, buffer.toString());

        return getPrefix(file);
    }

    /**
     * Method which the given file to the list of files
     * 
     * @param list
     *            The list of files
     * @param file
     *            The file
     * @return The new list
     */
    private File[] append(File[] files, File file) {
        File[] result = new File[files.length + 1];

        for (int i = 0; i < files.length; i++)
            result[i] = files[i];

        result[files.length] = file;
        return result;
    }

    /**
     * Gets the number of subdirectories in directory
     * 
     * @param dir
     *            the directory to check
     * @return int the number of directories
     */
    private int numDirectoriesDir(File dir) {
        return dir.listFiles(new DirectoryFilter()).length;
    }

    /**
     * Gets the number of files in directory (excluding directories)
     * 
     * @param dir
     *            the directory to check
     * @return int the number of files
     */
    private int numFilesDir(File dir) {
        return dir.listFiles(new FileFilter()).length;
    }

    /**
     * Returns whether or not the given filename represents a stored file
     * 
     * @param name
     *            The name of the file
     * @return int the number of files
     */
    private boolean isFile(File parent, String name) {
        // return (((getPrefix(parent).length() + name.length()) >= factory.getIdToStringLength()) && (!
        // name.equals(METADATA_FILENAME)));
        return ((!new File(parent, name).isDirectory()) && (!name.equals(METADATA_FILENAME)));
    }

    /**
     * Returns whether or not the given filename represents a directory
     * 
     * @param name
     *            The name of the file
     * @return int the number of files
     */
    private boolean isDirectory(File parent, String name) {
        // return (((getPrefix(parent).length() + name.length()) < factory.getIdToStringLength()) && (new File(parent,
        // name)).isDirectory());
        return (new File(parent, name)).isDirectory();
    }

    /*****************************************************************/
    /* Helper functions for Metadata Storage                         */
    /*****************************************************************/

    /**
     * Function which writes out all of the dirty metadata files and marks them as clean.
     */
    protected void writeDirty() {
        logger.log("writeDirty()");
        File[] files = (File[]) dirty.toArray(new File[0]);
        logger.log("files: " + Arrays.toString(files));

        for (int i = 0; i < files.length; i++) {
            HashMap map = new HashMap();
            IdRange range = getRangeForDirectory(files[i]);
            Iterator keys = null;

            // next line changed to cope with the case where the file IS the base directory
            // if (range.getCCWId().compareTo(range.getCWId()) <= 0) {
            if (range.getCCWId().compareTo(range.getCWId()) != 0) {
                keys = metadata.keySubMap(range.getCCWId(), range.getCWId()).keySet().iterator();
            } else {
                keys = metadata.keyTailMap(range.getCCWId()).keySet().iterator();
            }

            while (keys.hasNext()) {
                Id next = (Id) keys.next();
                map.put(next, metadata.get(next));
            }

            try {
                writeMetadataFile(files[i], map);

                synchronized (metadata) {
                    dirty.remove(files[i]);
                }
            } catch (FileNotFoundException f) {
                try {
                    synchronized (metadata) {
                        dirty.remove(files[i]);
                    }

                    if (logger.level <= Logger.WARNING)
                        logger.logException(String.format("ERROR: Could not find directory while writing out metadata in '%s ' - removing from dirty list and continuing!", files[i].getCanonicalPath()), f);
                } catch (IOException g) {
                    if (logger.level <= Logger.SEVERE)
                        logger.logException(String.format("PANIC: Got IOException %.50s trying to detail FNF exception %.50s while writing out file %s", g.getCause(), f.getCause(), files[i]), g);
                }
            } catch (IOException e) {
                try {
                    if (logger.level <= Logger.WARNING)
                        logger.logException(String.format("ERROR: Got error %.50s while writing out metadata in %s - aborting", e.getCause(), files[i].getCanonicalPath()), e);
                } catch (IOException f) {
                    if (logger.level <= Logger.SEVERE)
                        logger.logException(String.format("PANIC: Got IOException %.50s trying to detail exception %.50s while writing out file %s", f.getCause(), e.getCause(), files[i]), f);
                }
            }
        }
    }

    /**
     * Utility function which reads the metadata file off of disk and stores the result into the memory cache.
     * 
     * @param file
     *            The directory to read the metadata file for
     */
    private long readMetadataFile(File file) throws IOException {
        logger.log(String.format("readMetadataFile(%s)", file.getAbsolutePath()));
        File metadata = new File(file, METADATA_FILENAME);

        if (!metadata.exists())
            return -1L;

        FileInputStream fin = null;

        try {
            fin = new FileInputStream(metadata);
            ObjectInputStream objin = new ObjectInputStream(new BufferedInputStream(fin));

            IdRange range = getRangeForDirectory(file);

            try {
                HashMap map = (HashMap) objin.readObject();
                logger.log(map.toString());
                Iterator keys = map.keySet().iterator();

                while (keys.hasNext()) {
                    Id id = (Id) keys.next();

                    if ((range.containsId(id)) && (new File(file, id.toStringFull().substring(getPrefix(file).length())).exists()))
                        this.metadata.put(id, map.get(id));
                    else
                        dirty.add(file);
                }

                return metadata.lastModified();
            } catch (ClassNotFoundException e) {
                if (logger.level <= Logger.WARNING)
                    logger.logException("ERROR: Got exception " + e + " while reading metadata file " + metadata + " - rebuilding file", e);
                deleteFile(metadata);
                return 0L;
            } catch (IOException e) {
                if (logger.level <= Logger.WARNING)
                    logger.logException("ERROR: Got exception " + e + " while reading metadata file " + metadata + " - rebuilding file", e);
                deleteFile(metadata);
                return 0L;
            }
        } finally {
            fin.close();
        }
    }

    /**
     * Utility function which writes the metadata file out to disk.
     * 
     * @param file
     *            The directory to write the file to
     * @param map
     *            The data to write
     */
    private void writeMetadataFile(File file, HashMap map) throws IOException {
        logger.log(String.format("writeMetadataFile(%s, %s)", file.getAbsolutePath(), map));
        FileOutputStream fout = null;

        try {
            fout = new FileOutputStream(new File(file, METADATA_FILENAME));
            ObjectOutputStream objout = new ObjectOutputStream(new BufferedOutputStream(fout));
            objout.writeObject(map);
            objout.close();

        } finally {
            if (fout != null)
                fout.close();
        }
    }

    /**
     * Utility function which returns the range of keys which a directory corresponds to.
     * 
     * @param dir
     *            The directory
     */
    protected IdRange getRangeForDirectory(File dir) {
        String result = "";

        while (!dir.equals(appDirectory)) {
            result = dir.getName() + result;
            dir = dir.getParentFile();
        }

        return factory.buildIdRangeFromPrefix(result);
    }

    /*****************************************************************/
    /* Helper functions for Object Input/Output                      */
    /*****************************************************************/

    /**
     * Abstract over writing a single object to a file using Java serialization.
     * 
     * @param obj
     *            The object to be writen
     * @param file
     *            The file to serialize the object to.
     * @return The object's disk space usage
     */
    protected abstract long writeObject(Serializable obj, Serializable metadata, Id key, long version, File file) throws IOException;

    /**
     * Abstract over reading a single object to a file using Java serialization.
     * 
     * @param file
     *            The file to create the object from.
     * @return The object that was read in
     */
    protected abstract Serializable readData(File file) throws IOException;

    /**
     * Reads in the metadata from the provided file, or returns null if no metadata was found
     * 
     * @param file
     *            The file which should be read for the metadata
     */
    protected abstract Serializable readMetadata(File file) throws IOException;

    /**
     * Abstract over reading a single key from a file using Java serialization.
     * 
     * @param file
     *            The file to create the key from.
     * @return The key that was read in
     */
    protected Id readKey(File file) {
        String s = getPrefix(file.getParentFile()) + file.getName().replaceAll(ZERO_LENGTH_NAME, "");

        if (s.indexOf(".") >= 0) {
            return factory.buildIdFromToString(s.toCharArray(), 0, s.indexOf("."));
        } else {
            return factory.buildIdFromToString(s.toCharArray(), 0, s.length());
        }
    }

    /**
     * Abstract over reading a single key from a file using Java serialization.
     * 
     * @param file
     *            The file to create the key from.
     * @return The key that was read in
     */
    protected abstract Id readKeyFromFile(File file) throws IOException;

    /**
     * Abstract over reading a version from a file using Java serialization.
     * 
     * @param file
     *            The file to create the version from.
     * @return The key that was read in
     */
    protected abstract long readVersion(File file) throws IOException;

    /**
     * Re-writes the metadata stored in the provided file.
     * 
     * @param file
     *            The file to which the metadata should be written
     * @param metadata
     *            The metadata to write
     */
    protected abstract void writeMetadata(File file, Serializable metadata) throws IOException;

    /*****************************************************************/
    /* Functions for Configuration Management                        */
    /*****************************************************************/

    /**
     * Sets the root directory that the persistence Manager uses
     * 
     * @param dir
     *            the String representing the directory to use
     * @return boolean, true if the operation succeeds false if it doesn't
     */
    public boolean setRoot(String dir) {
        /* We should do something logical here to the existing files */
        rootDir = dir;
        return true;
    }

    /**
     * gets the root directory that the persistence Manager uses
     * 
     * @return String the directory for the root
     */
    public String getRoot() {
        return rootDir;
    }

    /**
     * gets the amount of storage that the persistence Manager uses
     * 
     * @return int the amount of storage in bytes allocated for use
     */
    public long getStorageSize() {
        if (storageSize > 0)
            return storageSize;
        else
            return Long.MAX_VALUE;
    }

    /**
     * Sets the amount of storage that the persistence Manager uses
     * 
     * @param size
     *            the amount of storage available to use in bytes
     * @return boolean, true if the operation succeeds false if it doesn't
     */
    public boolean setStorageSize(long size) {
        if (storageSize <= size) {
            storageSize = size;
            return true;
        } else if (size > usedSize) {
            storageSize = size;
            return true;
        } else {
            return false;
        }
    }

    /**
     * 
     * Increases the amount of storage recorded as used
     * 
     * @param long i the amount to increase usage by
     */
    private void increaseUsedSpace(long i) {
        usedSize = usedSize + i;
    }

    /**
     * 
     * decreases the amount of storage recorded as used
     * 
     * @param long i the amount to decrease usage by
     */
    private void decreaseUsedSpace(long i) {
        usedSize = usedSize - i;
    }

    /**
     * 
     * Gets the amount of space currently being used on disk
     * 
     * @return long the amount of space being used
     * 
     */
    private long getUsedSpace() {
        return usedSize;
    }

    /**
     * Gets the name of this instance
     * 
     * @return String the name of the instance
     * 
     */
    public String getName() {
        return name;
    }

    /*****************************************************************/
    /* Inner Classes for FileName filtering                          */
    /*****************************************************************/

    /**
     * A class that filters out the files in a directory to return only subdirectories.
     */
    private class DirectoryFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return isDirectory(dir, name); // (new File(dir, name)).isDirectory(); //((getPrefix(dir).length() +
            // name.length() < factory.getIdToStringLength()) && (new File(dir,
            // name)).isDirectory());
        }
    }

    /**
     * A class that filters out the files in a directory to return only files ( no directories )
     */
    private class FileFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return isFile(dir, name); // (getPrefix(dir).length() + name.length() >= factory.getIdToStringLength());
        }
    }

    /**
     * Class for a hashtable of primitive characters
     */
    private class CharacterHashSet {
        protected boolean[] bitMap = new boolean[256];

        public void put(char a) {
            bitMap[(int) a] = true;
        }

        public boolean contains(char a) {
            return bitMap[(int) a];
        }

        public void remove(char a) {
            bitMap[(int) a] = false;
        }

        public char[] get() {
            int[] nums = getOffsets();
            char[] result = new char[nums.length];

            for (int i = 0; i < result.length; i++)
                result[i] = (char) nums[i];

            return result;
        }

        private int[] getOffsets() {
            int[] result = new int[count()];

            for (int i = 0; i < result.length; i++)
                result[i] = getOffset(i);

            return result;
        }

        private int getOffset(int index) {
            int location = 0;

            /* skip the first index-1 values */
            while (index > 0) {
                if (bitMap[location])
                    index--;

                location++;
            }

            /* find the next true value */
            while (!bitMap[location])
                location++;

            return location;
        }

        private int count() {
            int total = 0;

            for (int i = 0; i < bitMap.length; i++)
                if (bitMap[i])
                    total++;

            return total;
        }
    }

    /*****************************************************************/
    /* Inner Classes for Worker Thread                               */
    /*****************************************************************/

    private static class PersistenceException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    private static class OutofDiskSpaceException extends PersistenceException {
        private static final long serialVersionUID = 1L;
    }
}
