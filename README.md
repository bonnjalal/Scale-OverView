# Scale-OverlayView
Scale Overlay View for Android view (xml) and jetpack Compose

This is a simple overlay view that the user can scale it as he want, and can put this view above other views like image and get scale or crop coordinates.

# usage in Android as view (xml)
First copy Android View/ScaleView.kt and Android View//values/attrs.xml files to your project.

Then in your layout file:

    <com.username.packagename.ScaleView
        android:id="@+id/draw_view"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:visibility="gone"
        app:ScaleView_crop_grid_size="2dp"
        app:ScaleView_dimmed_color="#6AFB5D51"
        app:ScaleView_enable_drag="true"
        app:ScaleView_grid_color="#F8F5F5"
        app:ScaleView_grid_column_count="3"
        app:ScaleView_grid_row_count="4"
        app:ScaleView_show_crop_grid="true"
        app:ScaleView_stroke_size="3dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />
        
And in yourjava or kotlin fragment or activity use it same as any other view.

# usage in jetpack compose :

First copy Compose View/ScaleOverlayView.kt to your project.

then use it as any other composable view:

    scaleOverView(context = activity,
                    enableDrag = true,
                    showGrid = true,
                    strokeColor = Color.White,
                    fillColor = Color(0x55A3B8F8),
                    gridLineCount = 4,
                    gridColor = Color.White,
                    strokeSize = 2F,
                    cornerBitmap = yourBitmap.asImageBitmap()
                    gridLineSize = 1F, cropBoxData = { cropRect ->
                        Log.e("CropRectData: ", "left: ${cropRect.left} \n " +
                                "right: ${cropRect.right} \n" +
                                "top: ${cropRect.top} \n " +
                                "bottom: ${cropRect.bottom} ")
                    })
 
        
       
