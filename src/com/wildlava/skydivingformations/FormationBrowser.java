//
// Skydiving Formations
//
// Copyright (C) 2013  Joe Peterson
//

package com.wildlava.skydivingformations;

//import android.util.Log;

import android.app.Activity;
import android.app.AlertDialog;
//import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Gallery;
import android.widget.ListView;
import android.widget.BaseAdapter;
import android.content.Context;
//import android.content.res.Configuration;
//import android.widget.FrameLayout;
//import android.widget.LinearLayout;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Toast;
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

public class FormationBrowser extends Activity
{
   public static final int DIVE_MAX_NUM_POINTS = 32;
   public static final int DEFAULT_FORMATION_SIZE = 8;
   
   public static final String EXTRA_MESSAGE_IMAGE_SIZE = "com.wildlava.skydivingformations.MESSAGE_IMAGE_SIZE";
   public static final String EXTRA_MESSAGE_NUM_POINTS = "com.wildlava.skydivingformations.MESSAGE_NUM_POINTS";
   public static final String EXTRA_MESSAGE_FORMATION_IDS = "com.wildlava.skydivingformations.MESSAGE_FORMATION_IDS";
   public static final String EXTRA_MESSAGE_FORMATION_NAMES = "com.wildlava.skydivingformations.MESSAGE_FORMATION_NAMES";
      
   int currentFormationSize = 0;
   //int pendingFormationSize = 0;
   int[] formationSize;
   int[] formationNum;
   String[] formationId;
   String[] formationName;

   int numEligibleFormations = 0;
   int[] eligibleFormations;
   int selectedFormation;
   boolean selectFormationPending = false;
   
   int formationImageSize = 0;
   int diveImageSize;
   //int formationThumbnailImageSize = 128; /* Note that the actual thumbnails
   //                                          need to be 86x86 to account for
   //                                          the gallery image borders. */
   //Bitmap[] formation_bitmaps;
   //Bitmap[] formationThumbnailBitmaps;
   ImageView formationView;
   ViewTreeObserver formationViewObserver;
   TextView formationNameView;
   //TextView formationIdView;
   
   int diveNumPoints = 0;
   //int[] diveFormations;
   String[] diveFormationIds;
   String[] diveFormationNames;
   
   Gallery gallery;
   FormationGalleryAdapter formationGalleryAdapter;
   
   Button clearDiveButton;
   Button addPointButton;
   Button diveViewButton;
   TextView diveNumPointsView;

   //boolean resumed = false;
   boolean newly_created = true;
   
   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);

      if (getResources().getConfiguration().orientation ==
          Configuration.ORIENTATION_LANDSCAPE)
      {
         //Log.v("Debug", "Landscape");
         setContentView(R.layout.pool_landscape);
      }
      else
      {
         //Log.v("Debug", "Portrait");
         setContentView(R.layout.pool);
      }

      readFormationIndex();

      // The "Pool View"
      gallery = (Gallery) findViewById(R.id.gallery);
      //gallery.setSaveEnabled(false);
      //gallery.setSpacing(1);
      formationView = (ImageView) findViewById(R.id.formation_view);
      formationNameView = (TextView) findViewById(R.id.formation_name_view);
      //formationIdView = (TextView) findViewById(R.id.formation_id_view);
      Spinner formationSizeSpinner = (Spinner) findViewById(R.id.formation_size_spinner);
      diveNumPointsView = (TextView) findViewById(R.id.dive_num_points_view);
      clearDiveButton = (Button) findViewById(R.id.clear_dive_button);
      addPointButton = (Button) findViewById(R.id.add_point_button);
      diveViewButton = (Button) findViewById(R.id.dive_view_button);

      // Set up a listener to get formation image view size
      int finalHeight, finalWidth;
      formationViewObserver = formationView.getViewTreeObserver();
      formationViewObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener()
         {
            public boolean onPreDraw()
            {
               int w = formationView.getMeasuredWidth();
               int h = formationView.getMeasuredHeight();
               if (h < w)
               {
                  formationImageSize = h;
               }
               else
               {
                  formationImageSize = w;
               }
               //Log.v("Debug", "viewW = " + w);
               //Log.v("Debug", "viewH = " + h);

               if (selectFormationPending)
               {
                  selectFormation(selectedFormation);
               }
               
               return true;
            }
         });
      
      // Find screen resolution to determine image scaling
      DisplayMetrics metrics = new DisplayMetrics();
      getWindowManager().getDefaultDisplay().getMetrics(metrics);
      if (getResources().getConfiguration().orientation ==
          Configuration.ORIENTATION_LANDSCAPE)
      {
         //formationImageSize = metrics.heightPixels - 240;
         diveImageSize = (int) ((float) metrics.heightPixels * 0.55);
      }
      else
      {
         /*
         int sizeByWidth = metrics.widthPixels - 40;
         int sizeByHeight = metrics.heightPixels - 340;
         if (sizeByHeight < sizeByWidth)
         {
            formationImageSize = sizeByHeight;
         }
         else
         {
            formationImageSize = sizeByWidth;
         }
         */
         diveImageSize = (int) ((float) metrics.widthPixels * 0.55);
      }

      //formationThumbnailImageSize = (int) ((double) formationViewSize / 3.75);
      
      // Set up the formation gallery widget
      formationGalleryAdapter = new FormationGalleryAdapter(this);
      gallery.setAdapter(formationGalleryAdapter);
      //gallery.setCallbackDuringFling(false);
      //gallery.setOnItemSelectedListener(new OnItemSelectedListener()
      gallery.setOnItemClickListener(new OnItemClickListener()
         {
            //public void onItemSelected(AdapterView parent, View v, int position, long id)
            public void onItemClick(AdapterView parent, View v, int position, long id)
            {
               selectFormation(position);
            }
            //public void onNothingSelected(AdapterView parent)
            //{
            //   selectFormation(0);
            //}
         });
      
      // Restore saved state
      if (savedInstanceState != null)
      {
         //Log.v("Debug", "onCreate setFormationSize(" + savedInstanceState.getInt("formationSize") + ")");
         setFormationSize(savedInstanceState.getInt("formationSize"), false);
         selectFormation(savedInstanceState.getInt("selectedFormation"));

         diveNumPoints = savedInstanceState.getInt("diveNumPoints");
         diveFormationIds = savedInstanceState.getStringArray("diveFormationIds");
         diveFormationNames = savedInstanceState.getStringArray("diveFormationNames");

         //for (int i=0; i<diveNumPoints; ++i)
         //{
         //   Log.v("Debug", "dive point restored: " + diveFormationNames[i]);
         //}

         if (diveNumPoints > 0)
         {
            clearDiveButton.setEnabled(true);
         }
      }
      else
      {
         //Log.v("Debug", "onCreate setFormationSize(" + DEFAULT_FORMATION_SIZE + ")");
         setFormationSize(DEFAULT_FORMATION_SIZE, true);

         diveFormationIds = new String[DIVE_MAX_NUM_POINTS];
         diveFormationNames = new String[DIVE_MAX_NUM_POINTS];
      }

      diveNumPointsView.setText("Dive points: " + diveNumPoints + " ");

      // Set up the formation size selector
      ArrayAdapter<CharSequence> formationSizeAdapter = ArrayAdapter.createFromResource(this, R.array.formation_size_array, android.R.layout.simple_spinner_item);
      formationSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      formationSizeSpinner.setAdapter(formationSizeAdapter);
      formationSizeSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
         {
            public void onItemSelected(AdapterView parent, View v, int position, long id)
            {
               setFormationSize(position + 2, true);
            }

            public void onNothingSelected(AdapterView parent)
            {
            }
         });

      if (savedInstanceState == null)
      {
         formationSizeSpinner.setSelection(DEFAULT_FORMATION_SIZE - 2);
      }
   }
   
   @Override
   protected void onStart()
   {
      super.onStart();

      if (newly_created)
      {
         // Display a dialog about the "Lite" version if applicable
         if (this.getClass().getCanonicalName().equals("com.wildlava.skydivingformationslite.FormationBrowser"))
         {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setNeutralButton("OK", null);
            dialog.setMessage("This is the \"Lite\" version of the app.  Except for 9-ways, this \"Lite\" version contains only 5 formations in each size.  The full version of the app includes the complete set of formations (over 1000 in total).").create().show();
         }
         
         newly_created = false;
      }
   }
   
   @Override
   protected void onSaveInstanceState(Bundle outState)
   {
      outState.putInt("formationSize", currentFormationSize);
      outState.putInt("selectedFormation", selectedFormation);
      outState.putInt("diveNumPoints", diveNumPoints);
      outState.putStringArray("diveFormationIds", diveFormationIds);
      outState.putStringArray("diveFormationNames", diveFormationNames);
      
      //for (int i=0; i<diveNumPoints; ++i)
      //{
      //   Log.v("Debug", "dive point saved: " + diveFormationNames[i]);
      //}
      
      super.onSaveInstanceState(outState);
   }
   
   /*
   @Override
   protected void onPause()
   {
      super.onPause();

      Log.v("Debug", "onPause()");
      resumed = false;
   }
   
   @Override
   protected void onResume()
   {
      super.onResume();

      resumed = true;
      Log.v("Debug", "onResume(): setFormationSize(" + pendingFormationSize + ") ; old=" + currentFormationSize);
      setFormationSize(pendingFormationSize, true);
   }
   */
   
   @Override
   protected void onActivityResult(int requestCode, int resultCode,
                                   Intent intent)
   {
      // Do not call super for now (which does nothing, anyway), since it
      // may not handle null intent in the future.
      //super.onActivityResult(requestCode, resultCode, intent);

      if (resultCode == RESULT_OK)
      {
         diveNumPoints = intent.getIntExtra(EXTRA_MESSAGE_NUM_POINTS, 0);
         diveFormationIds = intent.getStringArrayExtra(EXTRA_MESSAGE_FORMATION_IDS);
         diveFormationNames = intent.getStringArrayExtra(EXTRA_MESSAGE_FORMATION_NAMES);

         diveNumPointsView.setText("Dive points: " + diveNumPoints + " ");
      }
   }
   
   void readFormationIndex()
   {
      BufferedReader file = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.formation_index)));
      
      int num_formations = 0;
      try
      {
         while (file.readLine() != null)
            ++num_formations;
      }
      catch (java.io.IOException x)
      {
         try
         {
            file.close();
         }
         catch (java.io.IOException y)
         {
         }
         
         return;
      }
      
      try
      {
         file.close();
      }
      catch (java.io.IOException x)
      {
      }

      formationSize = new int[num_formations];
      formationNum = new int[num_formations];
      formationId = new String[num_formations];
      formationName = new String[num_formations];
      
      file = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.formation_index)));
      
      for (int i=0; i<num_formations; ++i)
      {
         String line;

         try
         {
            line = file.readLine();
         }
         catch (java.io.IOException x)
         {
            try
            {
               file.close();
            }
            catch (java.io.IOException y)
            {
            }
            
            return;
         }
         
         line = line.trim();

         formationSize[i] = Integer.parseInt(line.substring(0, 2));
         formationNum[i] = Integer.parseInt(line.substring(3, 6));
         formationId[i] = line.substring(0, 6);
         formationName[i] = line.substring(line.indexOf(" ") + 1);
      }

      try
      {
         file.close();
      }
      catch (java.io.IOException x)
      {
      }
   }

   void selectFormation(int n)
   {
      //if (n == selectedFormation)
      //{
      //   return;
      //}

      selectedFormation = n;

      if (formationImageSize == 0)
      {
         selectFormationPending = true;
         return;
      }
      else
      {
         selectFormationPending = false;
      }
      
      //Toast.makeText(FormationBrowser.this, "" + selectedFormation, Toast.LENGTH_SHORT).show();
      try
      {
         InputStream image_stream = getAssets().open(String.format("formations/%s.png", formationId[eligibleFormations[selectedFormation]]));
         formationView.setImageBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeStream(image_stream), formationImageSize, formationImageSize, true));
         //formationView.setImageBitmap(BitmapFactory.decodeStream(image_stream));
         image_stream.close();
      }
      catch (java.io.IOException x)
      {
      }
      
      formationNameView.setText(formationName[eligibleFormations[selectedFormation]]);
      //formationIdView.setText(formationId[eligibleFormations[selectedFormation]]);
      //Toast.makeText(FormationBrowser.this, formation_names[selectedFormation], Toast.LENGTH_SHORT).show();
   }

   void setFormationSize(int n, boolean reset)
   {
      //if (n == currentFormationSize || n == 0)
      if (n == currentFormationSize)
      {
         return;
      }

      //if (!resumed)
      //{
      //   pendingFormationSize = n;
      //   return;
      //}
      
      //Toast loading_msg = Toast.makeText(FormationBrowser.this,
      //                                   "Loading...", Toast.LENGTH_LONG);
      //loading_msg.show();
      
      currentFormationSize = n;
      //pendingFormationSize = 0;
      
      // Recycle old formation bitmaps if they exist
      //for (int i=0; i<numEligibleFormations; ++i)
      //{
      //   formationThumbnailBitmaps[i].recycle();
      //}
      
      // Select the formations of the given size
      numEligibleFormations = 0;
      for (int i=0; i<formationSize.length; ++i)
      {
         if (formationSize[i] == currentFormationSize)
            ++numEligibleFormations;
      }

      eligibleFormations = new int[numEligibleFormations];
      
      int j = 0;
      for (int i=0; i<formationSize.length; ++i)
      {
         if (formationSize[i] == currentFormationSize)
            eligibleFormations[j++] = i;
      }
      
      // Populate gallery bitmaps with scaled versions of
      // the selected formations.
      //formationThumbnailBitmaps = new Bitmap[numEligibleFormations];
   
      /*
      for (int i=0; i<numEligibleFormations; ++i)
      {
         try
         {
            InputStream image_stream = getAssets().open(String.format("formations/%s.png", formationId[eligibleFormations[i]]));
            Bitmap original_bitmap = BitmapFactory.decodeStream(image_stream);
            image_stream.close();
            //formation_bitmaps[i] = Bitmap.createScaledBitmap(original_bitmap, formationImageSize, formationImageSize, true);
            //formation_bitmaps[i] = original_bitmap;
            formationThumbnailBitmaps[i] = Bitmap.createScaledBitmap(original_bitmap, formationThumbnailImageSize, formationThumbnailImageSize, true);
            original_bitmap.recycle();
         }
         catch (java.io.IOException x)
         {
         }
      }
      */
      
      //loading_msg.cancel();

      if (reset)
      {
         //formationGalleryAdapter = new FormationGalleryAdapter(this);
         //gallery.setAdapter(formationGalleryAdapter);
         gallery.setSelection(0, false);
         formationGalleryAdapter.notifyDataSetChanged();
         selectFormation(0);
      }
      else
      {
         formationGalleryAdapter.notifyDataSetChanged();
      }
   }
   
   public void clearDive(View view)
   {
      diveNumPoints = 0;
      clearDiveButton.setEnabled(false);
      addPointButton.setEnabled(true);

      diveNumPointsView.setText("Dive points: 0 ");
   }
   
   public void addPoint(View view)
   {
      if (diveNumPoints < DIVE_MAX_NUM_POINTS)
      {
         int n = eligibleFormations[selectedFormation];
         
         //diveFormations[diveNumPoints] = n;
         diveFormationIds[diveNumPoints] = formationId[n];
         diveFormationNames[diveNumPoints] = formationName[n];
         //diveFormationImages[diveNumPoints] = formationName[n];

         ++diveNumPoints;
         
         //divePointsAdapter.notifyDataSetChanged();
         
         if (diveNumPoints == DIVE_MAX_NUM_POINTS)
         {
            addPointButton.setEnabled(false);
         }

         diveNumPointsView.setText("Dive points: " + diveNumPoints + " ");
         clearDiveButton.setEnabled(true);
      }
   }

   public void diveView(View view)
   {
      Intent intent = new Intent(this, DiveViewer.class);
      intent.putExtra(EXTRA_MESSAGE_IMAGE_SIZE, diveImageSize);
      intent.putExtra(EXTRA_MESSAGE_NUM_POINTS, diveNumPoints);
      intent.putExtra(EXTRA_MESSAGE_FORMATION_IDS, diveFormationIds);
      intent.putExtra(EXTRA_MESSAGE_FORMATION_NAMES, diveFormationNames);
      startActivityForResult(intent, 0);
   }
   
   public class FormationGalleryAdapter extends BaseAdapter
   {
      int mGalleryItemBackground;
      private Context mContext;
      
      public FormationGalleryAdapter(Context c)
      {
         mContext = c;
         TypedArray attr = mContext.obtainStyledAttributes(R.styleable.formation_gallery);
         mGalleryItemBackground = attr.getResourceId(R.styleable.formation_gallery_android_galleryItemBackground, 0);
         attr.recycle();
      }
      
      public int getCount()
      {
         return numEligibleFormations;
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
         ImageView imageView;

         if (convertView == null)
         {
            imageView = new ImageView(mContext);
            //imageView.setImageResource(formation_image_ids[position]);
            //imageView.setLayoutParams(new Gallery.LayoutParams(formationThumbnailImageSize, formationThumbnailImageSize));
            //Log.v("Debug", "thumb = " + formationThumbnailImageSize);
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            imageView.setBackgroundResource(mGalleryItemBackground);
         }
         else
         {
            imageView = (ImageView) convertView;
         }

         //imageView.setImageBitmap(formationThumbnailBitmaps[position]);
         try
         {
            InputStream image_stream = getAssets().open(String.format("formations/thumbs/%s.png", formationId[eligibleFormations[position]]));
            //imageView.setImageBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeStream(image_stream), 128, 128, true));
            imageView.setImageBitmap(BitmapFactory.decodeStream(image_stream));
            image_stream.close();
         }
         catch (java.io.IOException x)
         {
         }
         
         return imageView;
      }
   }
}
