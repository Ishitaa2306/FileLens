package com.example.intelligentfileorganizer.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.example.intelligentfileorganizer.data.local.dao.FileDao;
import com.example.intelligentfileorganizer.data.local.dao.FileDao_Impl;
import com.example.intelligentfileorganizer.data.local.dao.FolderDao;
import com.example.intelligentfileorganizer.data.local.dao.FolderDao_Impl;
import com.example.intelligentfileorganizer.data.local.dao.StorageConfigDao;
import com.example.intelligentfileorganizer.data.local.dao.StorageConfigDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class OrganizerDatabase_Impl extends OrganizerDatabase {
  private volatile StorageConfigDao _storageConfigDao;

  private volatile FolderDao _folderDao;

  private volatile FileDao _fileDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `storage_config` (`id` INTEGER NOT NULL, `treeUri` TEXT NOT NULL, `isConfigured` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `folders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `parentId` INTEGER, `documentId` TEXT NOT NULL, `documentUri` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`parentId`) REFERENCES `folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_folders_parentId` ON `folders` (`parentId`)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_folders_documentUri` ON `folders` (`documentUri`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `files` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `originalName` TEXT NOT NULL, `storedName` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `extension` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `documentUri` TEXT NOT NULL, `documentId` TEXT NOT NULL, `folderId` INTEGER NOT NULL, `importedAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`folderId`) REFERENCES `folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_files_folderId` ON `files` (`folderId`)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_files_documentUri` ON `files` (`documentUri`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'b758bc494e616922a95de153d9ec5002')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `storage_config`");
        db.execSQL("DROP TABLE IF EXISTS `folders`");
        db.execSQL("DROP TABLE IF EXISTS `files`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsStorageConfig = new HashMap<String, TableInfo.Column>(4);
        _columnsStorageConfig.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStorageConfig.put("treeUri", new TableInfo.Column("treeUri", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStorageConfig.put("isConfigured", new TableInfo.Column("isConfigured", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStorageConfig.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStorageConfig = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesStorageConfig = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoStorageConfig = new TableInfo("storage_config", _columnsStorageConfig, _foreignKeysStorageConfig, _indicesStorageConfig);
        final TableInfo _existingStorageConfig = TableInfo.read(db, "storage_config");
        if (!_infoStorageConfig.equals(_existingStorageConfig)) {
          return new RoomOpenHelper.ValidationResult(false, "storage_config(com.example.intelligentfileorganizer.data.local.entity.StorageConfigEntity).\n"
                  + " Expected:\n" + _infoStorageConfig + "\n"
                  + " Found:\n" + _existingStorageConfig);
        }
        final HashMap<String, TableInfo.Column> _columnsFolders = new HashMap<String, TableInfo.Column>(6);
        _columnsFolders.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFolders.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFolders.put("parentId", new TableInfo.Column("parentId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFolders.put("documentId", new TableInfo.Column("documentId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFolders.put("documentUri", new TableInfo.Column("documentUri", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFolders.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFolders = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysFolders.add(new TableInfo.ForeignKey("folders", "CASCADE", "NO ACTION", Arrays.asList("parentId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesFolders = new HashSet<TableInfo.Index>(2);
        _indicesFolders.add(new TableInfo.Index("index_folders_parentId", false, Arrays.asList("parentId"), Arrays.asList("ASC")));
        _indicesFolders.add(new TableInfo.Index("index_folders_documentUri", true, Arrays.asList("documentUri"), Arrays.asList("ASC")));
        final TableInfo _infoFolders = new TableInfo("folders", _columnsFolders, _foreignKeysFolders, _indicesFolders);
        final TableInfo _existingFolders = TableInfo.read(db, "folders");
        if (!_infoFolders.equals(_existingFolders)) {
          return new RoomOpenHelper.ValidationResult(false, "folders(com.example.intelligentfileorganizer.data.local.entity.FolderEntity).\n"
                  + " Expected:\n" + _infoFolders + "\n"
                  + " Found:\n" + _existingFolders);
        }
        final HashMap<String, TableInfo.Column> _columnsFiles = new HashMap<String, TableInfo.Column>(11);
        _columnsFiles.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFiles.put("originalName", new TableInfo.Column("originalName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFiles.put("storedName", new TableInfo.Column("storedName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFiles.put("mimeType", new TableInfo.Column("mimeType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFiles.put("extension", new TableInfo.Column("extension", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFiles.put("sizeBytes", new TableInfo.Column("sizeBytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFiles.put("documentUri", new TableInfo.Column("documentUri", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFiles.put("documentId", new TableInfo.Column("documentId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFiles.put("folderId", new TableInfo.Column("folderId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFiles.put("importedAt", new TableInfo.Column("importedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFiles.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFiles = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysFiles.add(new TableInfo.ForeignKey("folders", "CASCADE", "NO ACTION", Arrays.asList("folderId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesFiles = new HashSet<TableInfo.Index>(2);
        _indicesFiles.add(new TableInfo.Index("index_files_folderId", false, Arrays.asList("folderId"), Arrays.asList("ASC")));
        _indicesFiles.add(new TableInfo.Index("index_files_documentUri", true, Arrays.asList("documentUri"), Arrays.asList("ASC")));
        final TableInfo _infoFiles = new TableInfo("files", _columnsFiles, _foreignKeysFiles, _indicesFiles);
        final TableInfo _existingFiles = TableInfo.read(db, "files");
        if (!_infoFiles.equals(_existingFiles)) {
          return new RoomOpenHelper.ValidationResult(false, "files(com.example.intelligentfileorganizer.data.local.entity.FileEntity).\n"
                  + " Expected:\n" + _infoFiles + "\n"
                  + " Found:\n" + _existingFiles);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "b758bc494e616922a95de153d9ec5002", "041c32f182206da7c4522c328f322d57");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "storage_config","folders","files");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `storage_config`");
      _db.execSQL("DELETE FROM `folders`");
      _db.execSQL("DELETE FROM `files`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(StorageConfigDao.class, StorageConfigDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(FolderDao.class, FolderDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(FileDao.class, FileDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public StorageConfigDao storageConfigDao() {
    if (_storageConfigDao != null) {
      return _storageConfigDao;
    } else {
      synchronized(this) {
        if(_storageConfigDao == null) {
          _storageConfigDao = new StorageConfigDao_Impl(this);
        }
        return _storageConfigDao;
      }
    }
  }

  @Override
  public FolderDao folderDao() {
    if (_folderDao != null) {
      return _folderDao;
    } else {
      synchronized(this) {
        if(_folderDao == null) {
          _folderDao = new FolderDao_Impl(this);
        }
        return _folderDao;
      }
    }
  }

  @Override
  public FileDao fileDao() {
    if (_fileDao != null) {
      return _fileDao;
    } else {
      synchronized(this) {
        if(_fileDao == null) {
          _fileDao = new FileDao_Impl(this);
        }
        return _fileDao;
      }
    }
  }
}
