package com.example.intelligentfileorganizer.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.intelligentfileorganizer.data.local.entity.StorageConfigEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class StorageConfigDao_Impl implements StorageConfigDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<StorageConfigEntity> __insertionAdapterOfStorageConfigEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearConfig;

  public StorageConfigDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfStorageConfigEntity = new EntityInsertionAdapter<StorageConfigEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `storage_config` (`id`,`treeUri`,`isConfigured`,`updatedAt`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final StorageConfigEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTreeUri());
        final int _tmp = entity.isConfigured() ? 1 : 0;
        statement.bindLong(3, _tmp);
        statement.bindLong(4, entity.getUpdatedAt());
      }
    };
    this.__preparedStmtOfClearConfig = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM storage_config";
        return _query;
      }
    };
  }

  @Override
  public Object insertOrUpdate(final StorageConfigEntity config,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStorageConfigEntity.insert(config);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearConfig(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearConfig.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearConfig.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<StorageConfigEntity> observeStorageConfig() {
    final String _sql = "SELECT * FROM storage_config WHERE id = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"storage_config"}, new Callable<StorageConfigEntity>() {
      @Override
      @Nullable
      public StorageConfigEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTreeUri = CursorUtil.getColumnIndexOrThrow(_cursor, "treeUri");
          final int _cursorIndexOfIsConfigured = CursorUtil.getColumnIndexOrThrow(_cursor, "isConfigured");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final StorageConfigEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpTreeUri;
            _tmpTreeUri = _cursor.getString(_cursorIndexOfTreeUri);
            final boolean _tmpIsConfigured;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsConfigured);
            _tmpIsConfigured = _tmp != 0;
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new StorageConfigEntity(_tmpId,_tmpTreeUri,_tmpIsConfigured,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getStorageConfig(final Continuation<? super StorageConfigEntity> $completion) {
    final String _sql = "SELECT * FROM storage_config WHERE id = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<StorageConfigEntity>() {
      @Override
      @Nullable
      public StorageConfigEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTreeUri = CursorUtil.getColumnIndexOrThrow(_cursor, "treeUri");
          final int _cursorIndexOfIsConfigured = CursorUtil.getColumnIndexOrThrow(_cursor, "isConfigured");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final StorageConfigEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpTreeUri;
            _tmpTreeUri = _cursor.getString(_cursorIndexOfTreeUri);
            final boolean _tmpIsConfigured;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsConfigured);
            _tmpIsConfigured = _tmp != 0;
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new StorageConfigEntity(_tmpId,_tmpTreeUri,_tmpIsConfigured,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
