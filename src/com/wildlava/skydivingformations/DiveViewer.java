//
// Skydiving Formations
//
// Copyright (C) 2015  Joe Peterson
//

package com.wildlava.skydivingformations;

//import android.util.Log;

import android.app.Activity;
//import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Gallery;
import android.widget.ListView;
import android.widget.BaseAdapter;
import android.content.Context;
//import android.content.res.Configuration;
//import android.widget.FrameLayout;
//import android.widget.LinearLayout;
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
//import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Button;
//import android.widget.TabHost;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.TextView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
//import android.widget.EditText;
//import android.widget.Button;
//import android.view.View;
//import android.view.KeyEvent;
//import android.view.View.OnKeyListener;
//import android.view.View.OnClickListener;
//import android.view.Gravity;
//import android.view.inputmethod.InputMethodManager;
//import android.graphics.Typeface;
import java.io.InputStream;
import java.io.InputStreamReader;
//import java.io.FileReader;
import java.io.BufferedReader;
//import android.content.res.Resources.NotFoundException;

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

      // The "Dive View"
      divePointsView = (ListView) findViewById(R.id.dive_points_view);
      //divePointsView.addFooterView(findViewById(R.id.dive_footer_layout));
      divePointsView.addFooterView(getLayoutInflater().inflate(R.layout.dive_footer, divePointsView, false));
      ((TextView) findViewById(R.id.dive_view_notes)).setTextSize(TypedValue.COMPLEX_UNIT_PX, (int) (10.0 * textScaleFactor));
      poolViewButton = (Button) findViewById(R.id.pool_view_button);
      //poolViewButton = new Button(this);
      //divePointsView.addFooterView(poolViewButton);

      divePointsAdapter = new DivePointsAdapter(this);
      divePointsView.setAdapter(divePointsAdapter);
      registerForContextMenu(divePointsView);
   }

   @Override
   protected void onSaveInstanceState(Bundle outState)
   {
      outState.putInt("diveNumPoints", diveNumPoints);
      outState.putStringArray("diveFormationIds", diveFormationIds);
      outState.putStringArray("diveFormationNames", diveFormationNames);

      super.onSaveInstanceState(outState);
   }

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

   void updateFormation()
   {
      resultIntent = new Intent();
      resultIntent.putExtra(FormationBrowser.EXTRA_MESSAGE_NUM_POINTS, diveNumPoints);
      resultIntent.putExtra(FormationBrowser.EXTRA_MESSAGE_FORMATION_IDS, diveFormationIds);
      resultIntent.putExtra(FormationBrowser.EXTRA_MESSAGE_FORMATION_NAMES, diveFormationNames);

      setResult(RESULT_OK, resultIntent);
   }

   void deletePoint(int pointNum)
   {
      //if (diveNumPoints == 0)
      //   return;

      for (int i=pointNum; i<(diveNumPoints - 1); ++i)
      {
         diveFormationIds[i] = diveFormationIds[i + 1];
         diveFormationNames[i] = diveFormationNames[i + 1];
      }

      --diveNumPoints;

      divePointsAdapter.notifyDataSetChanged();
      updateFormation();
   }

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

   public void poolView(View view)
   {
      finish();
   }

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
            //imageView.setImageResource(formation_image_ids[position]);
            //imageView.setLayoutParams(new Gallery.LayoutParams(100, 100));
            //imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            //imageView.setBackgroundResource(mGalleryItemBackground);
         }
         else
         {
            v = convertView;
            imageView = (ImageView) v.findViewById(R.id.dive_point_image);
         }

         //Log.v("Debug", "DiveView: point num: " + v.findViewById(R.id.dive_point_num));
         //Log.v("Debug", "DiveView: point name: " + v.findViewById(R.id.dive_point_name));
         //Log.v("Debug", "DiveView: point id: " + v.findViewById(R.id.dive_point_id));
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
            //imageView.setImageBitmap(diveFormationImage[position]);
         }
         catch (java.io.IOException x)
         {
         }

         return v;
      }
   }
}
