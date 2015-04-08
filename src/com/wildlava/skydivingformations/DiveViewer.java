//
// Skydiving Formations
//
// Copyright (C) 2015  Joe Peterson
//

package com.wildlava.skydivingformations;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Gallery;
import android.widget.ListView;
import android.widget.BaseAdapter;
import android.content.Context;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Button;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.TextView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

/**
 * Provides the user with a constructed "dive" (skydive), allowing
 * the user to vertically scroll through the points of the dive
 * and make simple edits (to delete points or change their order).
 */
public class DiveViewer extends Activity
{
   float textScaleFactor;
   int diveImageSize;
   int diveNumPoints;
   String[] diveFormationIds;
   String[] diveFormationNames;

   Button poolViewButton;

   ListView divePointsView;
   DivePointsAdapter divePointsAdapter;

   Intent resultIntent;

   /**
    * Initializes the user interface, allowing viewing and editing of the dive.
    */
   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);

      Intent intent = getIntent();
      textScaleFactor = intent.getFloatExtra(FormationBrowser.EXTRA_MESSAGE_TEXT_SCALE_FACTOR, (float) 0.0);
      diveImageSize = intent.getIntExtra(FormationBrowser.EXTRA_MESSAGE_IMAGE_SIZE, 0);

      // Restore saved state
      if (savedInstanceState != null)
      {
         diveNumPoints = savedInstanceState.getInt("diveNumPoints");
         diveFormationIds = savedInstanceState.getStringArray("diveFormationIds");
         diveFormationNames = savedInstanceState.getStringArray("diveFormationNames");
      }
      else
      {
         diveNumPoints = intent.getIntExtra(FormationBrowser.EXTRA_MESSAGE_NUM_POINTS, 0);
         diveFormationIds = intent.getStringArrayExtra(FormationBrowser.EXTRA_MESSAGE_FORMATION_IDS);
         diveFormationNames = intent.getStringArrayExtra(FormationBrowser.EXTRA_MESSAGE_FORMATION_NAMES);
      }

      updateFormation();

      setContentView(R.layout.dive);

      // The "Dive View" (vertically-scrolling list of point images/info)
      divePointsView = (ListView) findViewById(R.id.dive_points_view);
      divePointsView.addFooterView(getLayoutInflater().inflate(R.layout.dive_footer, divePointsView, false));
      ((TextView) findViewById(R.id.dive_view_notes)).setTextSize(TypedValue.COMPLEX_UNIT_PX, (int) (10.0 * textScaleFactor));
      poolViewButton = (Button) findViewById(R.id.pool_view_button);

      divePointsAdapter = new DivePointsAdapter(this);
      divePointsView.setAdapter(divePointsAdapter);
      registerForContextMenu(divePointsView);
   }

   /**
    * Saves the the state of the activity, including the constructed dive,
    * so the user can resume viewing/editing the dive later.
    */
   @Override
   protected void onSaveInstanceState(Bundle outState)
   {
      outState.putInt("diveNumPoints", diveNumPoints);
      outState.putStringArray("diveFormationIds", diveFormationIds);
      outState.putStringArray("diveFormationNames", diveFormationNames);

      super.onSaveInstanceState(outState);
   }

   /**
    * Brings up a menu when the user long-presses.
    * This allows editing the dive.
    */
   @Override
   public void onCreateContextMenu(ContextMenu menu, View v,
                                   ContextMenuInfo menuInfo)
   {
      super.onCreateContextMenu(menu, v, menuInfo);

      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.dive_point_menu, menu);

      AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
      if (info.position == 0)
      {
         menu.findItem(R.id.dive_point_menu_move_up).setEnabled(false);
      }
      if (info.position == (diveNumPoints - 1))
      {
         menu.findItem(R.id.dive_point_menu_move_down).setEnabled(false);
      }
   }

   /**
    * Performs the edit that the user selects from the menu.
    */
   @Override
   public boolean onContextItemSelected(MenuItem item)
   {
      AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
      switch (item.getItemId())
      {
         case R.id.dive_point_menu_delete:
            deletePoint(info.position);
            return true;
         case R.id.dive_point_menu_move_up:
            movePointUp(info.position);
            return true;
         case R.id.dive_point_menu_move_down:
            movePointDown(info.position);
            return true;
         default:
            return super.onContextItemSelected(item);
      }
   }

   /**
    * Updates the info to be returned to the FormationBrowser activity after
    * an edit is performed by the user.
    */
   void updateFormation()
   {
      resultIntent = new Intent();
      resultIntent.putExtra(FormationBrowser.EXTRA_MESSAGE_NUM_POINTS, diveNumPoints);
      resultIntent.putExtra(FormationBrowser.EXTRA_MESSAGE_FORMATION_IDS, diveFormationIds);
      resultIntent.putExtra(FormationBrowser.EXTRA_MESSAGE_FORMATION_NAMES, diveFormationNames);

      setResult(RESULT_OK, resultIntent);
   }

   /**
    * Deletes the selected point from the dive.
    */
   void deletePoint(int pointNum)
   {
      for (int i=pointNum; i<(diveNumPoints - 1); ++i)
      {
         diveFormationIds[i] = diveFormationIds[i + 1];
         diveFormationNames[i] = diveFormationNames[i + 1];
      }

      --diveNumPoints;

      divePointsAdapter.notifyDataSetChanged();
      updateFormation();
   }

   /**
    * Swaps the selected point with the previous point.
    */
   void movePointUp(int pointNum)
   {
      String tmpFormationId = diveFormationIds[pointNum];
      String tmpFormationName = diveFormationNames[pointNum];
      diveFormationIds[pointNum] = diveFormationIds[pointNum - 1];
      diveFormationNames[pointNum] = diveFormationNames[pointNum - 1];
      diveFormationIds[pointNum - 1] = tmpFormationId;
      diveFormationNames[pointNum - 1] = tmpFormationName;

      divePointsAdapter.notifyDataSetChanged();
      updateFormation();
   }

   /**
    * Swaps the selected point with the next point.
    */
   void movePointDown(int pointNum)
   {
      String tmpFormationId = diveFormationIds[pointNum];
      String tmpFormationName = diveFormationNames[pointNum];
      diveFormationIds[pointNum] = diveFormationIds[pointNum + 1];
      diveFormationNames[pointNum] = diveFormationNames[pointNum + 1];
      diveFormationIds[pointNum + 1] = tmpFormationId;
      diveFormationNames[pointNum + 1] = tmpFormationName;

      divePointsAdapter.notifyDataSetChanged();
      updateFormation();
   }

   /**
    * Returns to the FormationViewer activity (caller).
    */
   public void poolView(View view)
   {
      finish();
   }

   /**
    * Provides an adapter to allow vertical scrolling of the dive point
    * images/info. Images are scaled in the app to correctly fit
    * the list views.
    */
   public class DivePointsAdapter extends BaseAdapter
   {
      private Context mContext;
      private LayoutInflater inflater;

      public DivePointsAdapter(Context c)
      {
         mContext = c;
         inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      }

      public int getCount()
      {
         return diveNumPoints;
      }

      public Object getItem(int position)
      {
         return position;
      }

      public long getItemId(int position)
      {
         return position;
      }

      public View getView(int position, View convertView, ViewGroup parent)
      {
         View v;
         ImageView imageView;

         if (convertView == null)
         {
            v = inflater.inflate(R.layout.dive_point, null);
            imageView = (ImageView) v.findViewById(R.id.dive_point_image);
         }
         else
         {
            v = convertView;
            imageView = (ImageView) v.findViewById(R.id.dive_point_image);
         }

         TextView divePointNum = (TextView) v.findViewById(R.id.dive_point_num);
         divePointNum.setText("Point " + (position + 1));
         divePointNum.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                                  (int) (10.0 * textScaleFactor));

         TextView divePointName = (TextView) v.findViewById(R.id.dive_point_name);
         divePointName.setText(diveFormationNames[position]);
         divePointName.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                                   (int) (15.0 * textScaleFactor));

         TextView divePointId = (TextView) v.findViewById(R.id.dive_point_id);
         divePointId.setText(diveFormationIds[position]);
         divePointId.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                                 (int) (10.0 * textScaleFactor));

         try
         {
            InputStream image_stream = mContext.getAssets().open(String.format("formations/%s.png", diveFormationIds[position]));
            imageView.setImageBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeStream(image_stream), diveImageSize, diveImageSize, true));
            image_stream.close();
         }
         catch (java.io.IOException x)
         {
         }

         return v;
      }
   }
}
