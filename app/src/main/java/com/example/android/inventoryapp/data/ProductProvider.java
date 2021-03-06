package com.example.android.inventoryapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

/**
 * Created by garywhite61 on 20/07/2017.
 * * {@link ContentProvider} for Inventory App.
 */

public class ProductProvider extends ContentProvider {

    /** Tag for the log messages */
    public static final String LOG_TAG = ProductProvider.class.getSimpleName();

    /** URI matcher code for the content URI for the PRODUCTS table */
    private static final int PRODUCTS = 100;

    /** URI matcher code for the content URI for a single item in the PRODUCTS table */
    private static final int PRODUCT_ID = 101;

    /** UriMatcher object to match a content URI to a corresponding code */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    /** Database Helper Object */
    private ProductDbHelper mDbHelper;

    /** Static initializer - run the first time anything is called from this class */
    static {
        // Content URI of the form "content://package-name/products"
        // This URI is used to provide access to multiple table rows.
        sUriMatcher.addURI(ProductContract.CONTENT_AUTHORITY, ProductContract.PATH_PRODUCTS, PRODUCTS);

        // Content URI of the form "content://package-name/products/#", where # is the ID value
        // This is used to provide access to single row of the table.
        sUriMatcher.addURI(ProductContract.CONTENT_AUTHORITY, ProductContract.PATH_PRODUCTS + "/#", PRODUCT_ID);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new ProductDbHelper(getContext());
        return true;
    }

    /**
     * Function - READ from table
     * Peform query for given URI and load the cursor with results fetched from table.
     * The returned result can have multiple rows or a single row, depending on given URI.
     * @param uri
     * @param projection
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @return cursor
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        // Get instance of readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // Cursor to hold the query result
        Cursor cursor;

        switch (sUriMatcher.match(uri)) {
            case PRODUCTS:
                cursor = database.query(
                        ProductContract.ProductEntry.TABLE_NAME,
                        projection, selection, selectionArgs, null, null, sortOrder);
                break;

            case PRODUCT_ID:
                selection = ProductContract.ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                cursor = database.query(ProductContract.ProductEntry.TABLE_NAME,
                        projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on Cursor so it knows when to update in the event the data in cursor changes
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    /**
     * Function - INSERT into table
     * This method inserts records in the table
     * @param uri
     * @param contentValues
     * @return uri
     */
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return insertProduct(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    private Uri insertProduct(Uri uri, ContentValues values) {
        String name = values.getAsString(ProductContract.ProductEntry.COLUMN_INVENTORY_NAME);
        Integer price = values.getAsInteger(ProductContract.ProductEntry.COLUMN_INVENTORY_PRICE);
        Integer quantity = values.getAsInteger(ProductContract.ProductEntry.COLUMN_INVENTORY_QUANTITY);

        if (name == null) {
            throw new IllegalArgumentException("Product requires a name");
        }

        if (price != null && price < 0) {
            throw new IllegalArgumentException("Product requires a valid price");
        }

        if (quantity == null) {
            throw new IllegalArgumentException("Product requires a valid quantity");
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        long newRowId = db.insert(ProductContract.ProductEntry.TABLE_NAME, null, values);

        // Check if ID is -1, which means record insert has failed
        if (newRowId == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID of the newly inserted row appended at the end
        return ContentUris.withAppendedId(uri, newRowId);
    }

    /**
     * Function - UPDATE table
     * This method updates products
     * @param uri
     * @param contentValues
     * @param selection
     * @param selectionArgs
     * @return int
     */
    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return updateProduct(uri, contentValues, selection, selectionArgs);
            case PRODUCT_ID:
                selection = ProductContract.ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateProduct(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * This method validates input data for updates and applies the updates
     * @param uri
     * @param values
     * @param selection
     * @param selectionArgs
     * @return
     */
    private int updateProduct(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }
        if (values.containsKey(ProductContract.ProductEntry.COLUMN_INVENTORY_NAME)) {
            String name = values.getAsString(ProductContract.ProductEntry.COLUMN_INVENTORY_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Product requires a name");
            }
        }
        if (values.containsKey(ProductContract.ProductEntry.COLUMN_INVENTORY_PRICE)) {
            Integer price = values.getAsInteger(ProductContract.ProductEntry.COLUMN_INVENTORY_PRICE);
            if (price != null && price < 0) {
                throw new IllegalArgumentException("Product requires valid price");
            }
        }
        if (values.containsKey(ProductContract.ProductEntry.COLUMN_INVENTORY_QUANTITY)) {
            Integer quantity = values.getAsInteger(ProductContract.ProductEntry.COLUMN_INVENTORY_QUANTITY);
            if (quantity == null) {
                throw new IllegalArgumentException("Product requires valid quantity");
            }
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        int numberRowsUpdated = db.update(ProductContract.ProductEntry.TABLE_NAME, values, selection, selectionArgs);

        if (numberRowsUpdated == 0) {
            Log.e(LOG_TAG, "No rows updated for " + uri);
        } else {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return numberRowsUpdated;
    }

    /**
     * Function - DELETE from table
     * This method deletes records from the table
     * @param uri
     * @param selection
     * @param selectionArgs
     * @return number of rows deleted
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = db.delete(ProductContract.ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;

            case PRODUCT_ID:
                // Delete a single row given by the ID in the URI
                selection = ProductContract.ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = db.delete(ProductContract.ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return ProductContract.ProductEntry.CONTENT_LIST_TYPE;
            case PRODUCT_ID:
                return ProductContract.ProductEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}