package net.johnhany.moaap_chp2_r3;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.annotation.NonNull;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity
{
    private Mat originalMat;

    private Bitmap currentBitmap;
    private ImageView imageView;

    private final int ACTION_PICK_PHOTO = 1;
    static int REQUEST_READ_EXTERNAL_STORAGE = 0;
    static boolean read_external_storage_granted = false;

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this)
    {
        @Override
        public void onManagerConnected( int status )
        {
            switch ( status ) {
                case LoaderCallbackInterface.SUCCESS:
                    //DO YOUR WORK/STUFF HERE
                    Log.i("OpenCV", "OpenCV loaded successfully.");
                    break;
                default:
                    super.onManagerConnected( status );
                    break;
            }
        }
    };

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        imageView = (ImageView)findViewById( R.id.image_view );

        OpenCVLoader.initAsync( OpenCVLoader.OPENCV_VERSION_3_1_0, this, mOpenCVCallBack );

        if ( ContextCompat.checkSelfPermission( MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE )
                != PackageManager.PERMISSION_GRANTED ) {
            Log.i("permission", "request READ_EXTERNAL_STORAGE");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_READ_EXTERNAL_STORAGE);
        }else {
            Log.i("permission", "READ_EXTERNAL_STORAGE already granted");
            read_external_storage_granted = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.open_gallery) {
            if(read_external_storage_granted) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, ACTION_PICK_PHOTO);
            }else {
                return true;
            }
        } else if (id == R.id.DoG) {
            if ( imageView.getDrawable()  == null ) {
                Toast.makeText(this, "Para aplicar Diferença Gaussiana selecione uma imagem primeiro, animal estúpido", Toast.LENGTH_SHORT).show();
            }
            else {
                DifferenceOfGaussian();
            }
        } else if (id == R.id.CannyEdges) {
            if ( imageView.getDrawable()  == null ) {
                Toast.makeText(this, "Para aplicar Canny Edge detection selecione uma imagem primeiro, animal estúpido", Toast.LENGTH_SHORT).show();
            }
            else {
                Canny();
            }

        } else if (id == R.id.SobelFilter) {
            if ( imageView.getDrawable()  == null ) {
                Toast.makeText(this, "Para aplicar filtro sobel selecione uma imagem primeiro, animal estúpido", Toast.LENGTH_SHORT).show();
            }
            else {
                Sobel();
            }
        } else if (id == R.id.HarrisCorners) {
            if ( imageView.getDrawable()  == null ) {
                Toast.makeText(this, "Para aplicar HarisCorner detection selecione uma imagem primeiro, animal estúpido", Toast.LENGTH_SHORT).show();
            }
            else {
                HarrisCorner();
            }
        } else if (id == R.id.HoughLines) {
            if ( imageView.getDrawable()  == null ) {
                Toast.makeText(this, "Para aplicar Hough lines detecton selecione uma imagem primeiro, animal estúpido", Toast.LENGTH_SHORT).show();
            }
            else {
                HoughLines();
            }
        } else if (id == R.id.HoughCircles) {
            if ( imageView.getDrawable()  == null ) {
                Toast.makeText(this, "Para aplicar HoughCircles detection selecione uma imagem primeiro, animal estúpido", Toast.LENGTH_SHORT).show();
            }
            else {
                HoughCircles();
            }
        } else if (id == R.id.Contours) {
            if ( imageView.getDrawable()  == null ) {
                Toast.makeText(this, "Para aplicar Contours detection selecione uma imagem primeiro, animal estúpido", Toast.LENGTH_SHORT).show();
            }
            else {
                Contours();
            }
        }
        else if ( id == R.id.TestOMR ) {

            if ( imageView.getDrawable()  == null ) {
                Toast.makeText(this, "Para testar OMR, selecione uma imagem do gabarito", Toast.LENGTH_SHORT).show();
            }
            else {
                TestOMR();
            }
        }
        else if ( id == R.id.CorrecaoProva ) {
            if ( imageView.getDrawable()  == null ) {
                Toast.makeText(this, "Para Analizar Gabarito, selecione uma imagem do gabarito", Toast.LENGTH_SHORT).show();
            }
            else {
                CorrigirProva();
            }
        }

        return super.onOptionsItemSelected(item);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTION_PICK_PHOTO && resultCode == RESULT_OK && null != data && read_external_storage_granted) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            String picturePath;
            if(cursor == null) {
                Log.i("data", "cannot load any image");
                return;
            }else {
                try {
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    picturePath = cursor.getString(columnIndex);
                }finally {
                    cursor.close();
                }
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            Bitmap temp = BitmapFactory.decodeFile(picturePath, options);

            int orientation = 0;

            try {
                ExifInterface imgParams = new ExifInterface(picturePath);
                orientation = imgParams.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Matrix rotate90 = new Matrix();
            rotate90.postRotate(orientation);
            Bitmap originalBitmap = rotateBitmap(temp,orientation);

            if(originalBitmap != null) {
                Bitmap tempBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                originalMat = new Mat(tempBitmap.getHeight(),
                        tempBitmap.getWidth(), CvType.CV_8U);
                Utils.bitmapToMat(tempBitmap, originalMat);
                currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, false);
                imageView.setImageBitmap(currentBitmap);
            }else {
                Log.i("data", "originalBitmap is empty");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                Log.i("permission", "READ_EXTERNAL_STORAGE granted");
                read_external_storage_granted = true;
            } else {
                // permission denied
                Log.i("permission", "READ_EXTERNAL_STORAGE denied");
            }
        }
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation)
    {
        Matrix matrix = new Matrix();

        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }

    }

    //Difference of Gaussian
    public void DifferenceOfGaussian()
    {
        Mat grayMat = new Mat();
        Mat blur1 = new Mat();
        Mat blur2 = new Mat();

        //Converting the image to grayscale
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_BGR2GRAY);

        Imgproc.GaussianBlur(grayMat, blur1, new Size(15, 15), 5);
        Imgproc.GaussianBlur(grayMat, blur2, new Size(21, 21), 5);

        //Subtracting the two blurred images
        Mat DoG = new Mat();
        Core.absdiff(blur1, blur2, DoG); // a app tah quebrando nessa linha

        Toast.makeText(this, "O leão que ruge na noite sombria", Toast.LENGTH_SHORT).show();

        //Inverse Binary Thresholding
        Core.multiply(DoG, new Scalar(100), DoG);
        Imgproc.threshold(DoG, DoG, 50, 255, Imgproc.THRESH_BINARY_INV);

        //Converting Mat back to Bitmap
        Utils.matToBitmap(DoG, currentBitmap);
        imageView.setImageBitmap(currentBitmap);
    }

    //Canny Edge Detection
    void Canny()
    {
        Mat grayMat = new Mat();
        Mat cannyEdges = new Mat();

        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.Canny(grayMat, cannyEdges, 10, 100);

        Utils.matToBitmap(cannyEdges, currentBitmap);
        imageView.setImageBitmap(currentBitmap);
    }

    //Sobel Operator
    void Sobel()
    {
        Mat grayMat = new Mat();
        Mat sobel = new Mat(); //Mat to store the final result

        //Matrices to store gradient and absolute gradient respectively
        Mat grad_x = new Mat();
        Mat abs_grad_x = new Mat();

        Mat grad_y = new Mat();
        Mat abs_grad_y = new Mat();

        //Converting the image to grayscale
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_BGR2GRAY);

        //Calculating gradient in horizontal direction
        Imgproc.Sobel(grayMat, grad_x, CvType.CV_16S, 1, 0, 3, 1, 0);

        //Calculating gradient in vertical direction
        Imgproc.Sobel(grayMat, grad_y, CvType.CV_16S, 0, 1, 3, 1, 0);

        //Calculating absolute value of gradients in both the direction
        Core.convertScaleAbs(grad_x, abs_grad_x);
        Core.convertScaleAbs(grad_y, abs_grad_y);

        //Calculating the resultant gradient
        Core.addWeighted(abs_grad_x, 0.5, abs_grad_y, 0.5, 1, sobel);

        //Converting Mat back to Bitmap
        Utils.matToBitmap(sobel, currentBitmap);
        imageView.setImageBitmap(currentBitmap);
    }

    void HoughLines()
    {
        Mat grayMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat lines = new Mat();

        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(grayMat, cannyEdges, 10, 100);
        Imgproc.HoughLinesP(cannyEdges, lines, 1, Math.PI / 180, 50, 20, 20);

        Mat houghLines = new Mat();
        houghLines.create(cannyEdges.rows(), cannyEdges.cols(), CvType.CV_8UC1);

        //Drawing lines on the image
        for (int i = 0; i < lines.cols(); i++) {
            double[] points = lines.get(0, i);
            double x1, y1, x2, y2;

            x1 = points[0];
            y1 = points[1];
            x2 = points[2];
            y2 = points[3];

            Point pt1 = new Point(x1, y1);
            Point pt2 = new Point(x2, y2);

            //Drawing lines on an image
            Imgproc.line(houghLines, pt1, pt2, new Scalar(255, 0, 0), 1);
        }

        //Converting Mat back to Bitmap
        Utils.matToBitmap(houghLines, currentBitmap);
        imageView.setImageBitmap(currentBitmap);

    }

    void HoughCircles()
    {
        Mat grayMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat circles = new Mat();

        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(grayMat, cannyEdges, 10, 100);
        Imgproc.HoughCircles(cannyEdges, circles, Imgproc.CV_HOUGH_GRADIENT, 1, cannyEdges.rows() / 15);//, grayMat.rows() / 8);

        Mat houghCircles = new Mat();
        houghCircles.create(cannyEdges.rows(), cannyEdges.cols(), CvType.CV_8UC1);

        //Drawing lines on the image
        for (int i = 0; i < circles.cols(); i++) {
            double[] parameters = circles.get(0, i);
            double x, y;
            int r;

            x = parameters[0];
            y = parameters[1];
            r = (int) parameters[2];

            Point center = new Point(x, y);

            //Drawing circles on an image
            Imgproc.circle(houghCircles, center, r, new Scalar(255, 0, 0), 1);
        }

        //Converting Mat back to Bitmap
        Utils.matToBitmap(houghCircles, currentBitmap);
        imageView.setImageBitmap(currentBitmap);
    }

    void Contours()
    {
        Mat grayMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat hierarchy = new Mat();

        List<MatOfPoint> contourList = new ArrayList<MatOfPoint>(); //A list to store all the contours

        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(originalMat, cannyEdges, 10, 100);
        Imgproc.findContours(cannyEdges, contourList, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        //Drawing contours on a new image
        Mat contours = new Mat();
        contours.create(cannyEdges.rows(), cannyEdges.cols(), CvType.CV_8UC3);
        Random r = new Random();

        for (int i = 0; i < contourList.size(); i++) {
            Imgproc.drawContours(contours, contourList, i, new Scalar(r.nextInt(255), r.nextInt(255), r.nextInt(255)), -1);
        }

        //Converting Mat back to Bitmap
        Utils.matToBitmap(contours, currentBitmap);
        imageView.setImageBitmap(currentBitmap);
    }

    void HarrisCorner()
    {
        Mat grayMat = new Mat();
        Mat corners = new Mat();

        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        Mat tempDst = new Mat();
        Imgproc.cornerHarris(grayMat, tempDst, 2, 3, 0.04);

        //Normalizing harris corner's output
        Mat tempDstNorm = new Mat();
        Core.normalize(tempDst, tempDstNorm, 0, 255, Core.NORM_MINMAX);
        Core.convertScaleAbs(tempDstNorm, corners);

        //Drawing corners on a new image
        Random r = new Random();

        for (int i = 0; i < tempDstNorm.cols(); i++) {
            for (int j = 0; j < tempDstNorm.rows(); j++) {
                double[] value = tempDstNorm.get(j, i);
                if (value[0] > 150)
                    Imgproc.circle(corners, new Point(i, j), 5, new Scalar(r.nextInt(255)), 2);
            }
        }

        //Converting Mat back to Bitmap
        Utils.matToBitmap(corners, currentBitmap);
        imageView.setImageBitmap(currentBitmap);
    }

    void HOGDescriptor()
    {
        Mat grayMat = new Mat();
        Mat people = new Mat();

        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        HOGDescriptor hog = new HOGDescriptor();
        hog.setSVMDetector(HOGDescriptor.getDefaultPeopleDetector());

        MatOfRect faces = new MatOfRect();
        MatOfDouble weights = new MatOfDouble();

        hog.detectMultiScale(grayMat, faces, weights);
        originalMat.copyTo(people);
        Rect[] facesArray = faces.toArray();

        for (int i = 0; i < facesArray.length; i++)
            Imgproc.rectangle(people, facesArray[i].tl(), facesArray[i].br(), new Scalar(100), 3);

        Utils.matToBitmap(people, currentBitmap);
        imageView.setImageBitmap(currentBitmap);
    }

    public void TestOMR()
    {
        Mat grayMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat hierarchy = new Mat();
        Mat threshHold = new Mat();
        Mat blurredImage = new Mat();

        int numMarkedCircle = 0;
        int lineWatcher = 1;
        int labelQuestion = 0;

        List<MatOfPoint> contourList = new ArrayList<MatOfPoint>();
        List<MatOfPoint> circlesList = new ArrayList<MatOfPoint>();
        List<MatOfPoint> sortedCirclesList = new ArrayList<MatOfPoint>();

        // Pré-processa a imagem
        Imgproc.cvtColor( originalMat, grayMat, Imgproc.COLOR_BGR2GRAY );
        Imgproc.GaussianBlur( grayMat, blurredImage, new Size( 5, 5 ), 0 );
        Imgproc.Canny( blurredImage, cannyEdges, 75, 200 );
        Imgproc.threshold( blurredImage, threshHold, 0, 255, Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU );

        // Encontra todos os contornos na imagem
        Imgproc.findContours(threshHold.clone(), contourList, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE );

        // Encontrando os círculos dentre os contornos encontrados
        for ( MatOfPoint contour : contourList )
        {
            Rect boundBoxing = Imgproc.boundingRect( contour );
            Float ar = boundBoxing.width / (float) boundBoxing.height;

            //Toast.makeText(this, "Altura do contorno " + Integer.toString( boundBoxing.height ), Toast.LENGTH_SHORT).show();
            //Toast.makeText(this, "Largura do contorno " + Integer.toString( boundBoxing.width ), Toast.LENGTH_SHORT).show();

            // Avalia se o contorno limitado pelo retângulo é um círculo
            if (  boundBoxing.width >= 34 && boundBoxing.height >= 34 && ar >= 0.9 && ar <= 1.1 )
            {
                circlesList.add( contour );
                Mat mask = threshHold.submat( boundBoxing );
                //Toast.makeText(this, "Número de pixels brancos no círculo: " + Integer.toString( numCirclesDetectedFromContourList++ ) + " => " + Integer.toString( Core.countNonZero( mask ) ), Toast.LENGTH_SHORT).show();

                if ( Core.countNonZero( mask ) >= 403 ) {
                    numMarkedCircle++;
                }
            }
        }

        Toast.makeText(this, "Tamanho exato da circlesList: " + Integer.toString( circlesList.size() ), Toast.LENGTH_SHORT).show();

        List<MatOfPoint> sortedLineList = new ArrayList<MatOfPoint>();


        int contadorImparcialCirculosDetectados = 0;

        ///* Pega os círculos linha por linha, ordena e inseri na lista de círculos ordenados
        for ( MatOfPoint circle : circlesList )
        {
            contadorImparcialCirculosDetectados++;

            if ( lineWatcher < 6 ) {
                sortedLineList.add( circle );
                lineWatcher++;

                // Aqui soh entrará na última interação do for mais externo
                if ( contadorImparcialCirculosDetectados > circlesList.size() - 1 ) {
                    for ( MatOfPoint circleFromLine : sortedLineList ) {
                        sortedCirclesList.add( circleFromLine );
                    }
                }
            }
            else {
                Collections.sort( sortedLineList, new Comparator<MatOfPoint>()
                {
                    @Override
                    public int compare(MatOfPoint mop1, MatOfPoint mop2)
                    {
                        long sumMop1 = 0;
                        long sumMop2 = 0;

                        for( Point p: mop1.toList() ){
                            sumMop1 += p.x;
                        }

                        for( Point p: mop2.toList() ){
                            sumMop2 += p.x;
                        }

                        if( sumMop1 > sumMop2)
                            return -1;
                        else if( sumMop1 < sumMop2 )
                            return 1;
                        else
                            return 0;
                    }
                });

                for ( MatOfPoint circleFromLine : sortedLineList ) {
                    sortedCirclesList.add( circleFromLine );
                }

                lineWatcher = 1;
                sortedLineList = new ArrayList<>();
            }
        }

        Toast.makeText(this, "Contador Imparcial: " + Integer.toString( contadorImparcialCirculosDetectados ), Toast.LENGTH_SHORT).show();

        int thickness = 0;

        // Desenha um ponto vermelho no centro dos círculos encontrados
        Collections.reverse( sortedCirclesList );
        for ( MatOfPoint circle : circlesList )
        {
            Rect rect = Imgproc.boundingRect( circle );

            Point point0 = rect.tl();
            Point point = rect.br();
            Point pontoMedio = new Point(  ( point0.x + point.x ) / 2, ( point0.y + point.y ) / 2 );

            Imgproc.circle( threshHold, pontoMedio, 1, new Scalar( 218, 23, 4 ), thickness );
            //Toast.makeText( this, "Coordenadas do Centro do Círculo " + Integer.toString( countCircle++ ) + " => " + " ( x = " + Double.toString( pontoMedio.x ) + ", y = " + Double.toString( pontoMedio.y ) + " )", Toast.LENGTH_SHORT ).show();
            //alternativas += Integer.toString( labelQuestion++ ) + " ";
            thickness += 2;
        }

        // Exibinto via Toast as estatísiticas
        //Toast.makeText(this, "Total de Contornos na imagem segmentada: " + Integer.toString( contourList.size() ), Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "Total de Círculos Encontrados: " + Integer.toString( sortedCirclesList.size() ), Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "Número de Questões: " + Integer.toString( circlesList.size() / 5 ), Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "Número de Alternativas Marcadas: " + Integer.toString( numMarkedCircle ), Toast.LENGTH_SHORT).show();
        //Toast.makeText(this, "Etiqueta das Alternativas: " + alternativas, Toast.LENGTH_SHORT).show();
        //Toast.makeText(this, "Alternativas: " + alternativas, Toast.LENGTH_SHORT).show();

        // Exibe o resultado da imagem trabalhada
        Utils.matToBitmap( threshHold, currentBitmap );
        imageView.setImageBitmap( currentBitmap);

    } //Fim de TestOMR


    // Correção da Prova, análise de gabarito
    private void CorrigirProva()
    {
        Mat grayMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat hierarchy = new Mat();
        Mat threshHold = new Mat();
        Mat blurredImage = new Mat();

        List<MatOfPoint> contourList = new ArrayList<MatOfPoint>();
        List<MatOfPoint> circlesList = new ArrayList<MatOfPoint>();
        List<MatOfPoint> markedCirclesList = new ArrayList<MatOfPoint>();
        List<MatOfPoint> rectRefsList = new ArrayList<MatOfPoint>();

        // Pré-processa a imagem
        Imgproc.cvtColor( originalMat, grayMat, Imgproc.COLOR_BGR2GRAY );
        Imgproc.GaussianBlur( grayMat, blurredImage, new Size( 5, 5 ), 0 );
        Imgproc.Canny( blurredImage, cannyEdges, 75, 200 );
        Imgproc.threshold( blurredImage, threshHold, 0, 255, Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU );

        // Encontra todos os contornos da imagem
        Imgproc.findContours( threshHold.clone(), contourList, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE );

        // Listas das alturas e larguras dos retângulos que envolvem os contornos
        ArrayList<Integer> rectHeightstList = new ArrayList<>();
        ArrayList<Integer> rectWidthstList = new ArrayList<>();

        // cria as listas de alturas e larguras dos retângulos utilizados para parametrizar identificação de círculos
        Iterator<MatOfPoint> iterador = contourList.iterator();
        while ( iterador.hasNext() ) {
            MatOfPoint tmpContour = iterador.next();
            Rect tmpRect = Imgproc.boundingRect( tmpContour );

            rectHeightstList.add( tmpRect.height );
            rectWidthstList.add( tmpRect.width );

        }

        // Ordena lista de alturas dos retângulos para parametrizar identificador de círculos
        Collections.sort( rectHeightstList, new Comparator<Integer>()
        {
            @Override
            public int compare(Integer int1, Integer int2)
            {

                if ( int1 > int2 ) {
                    return 1;
                }
                else if ( int1 < int2 ) {
                    return -1;
                }
                else {
                    return 0;
                }

            }
        });

        // Ordena lista de larguras dos retângulos para parametrizar identificador de círculos
        Collections.sort( rectWidthstList, new Comparator<Integer>()
        {
            @Override
            public int compare(Integer int1, Integer int2)
            {

                if ( int1 > int2 ) {
                    return 1;
                }
                else if ( int1 < int2 ) {
                    return -1;
                }
                else {
                    return 0;
                }

            }
        });


        // Menores alturas e larguras de contornos para identificação dos círculos
        int minContourHeight = rectHeightstList.get( 0 ) + 2;
        int minContourWidth = rectWidthstList.get( 0 ) + 2;

        // Encontrando os círculos e os retângulos de referência dentre os contornos encontrados
        for ( MatOfPoint contour : contourList )
        {
            Rect boundCircleBoxing = Imgproc.boundingRect( contour );
            Float ar = (float) boundCircleBoxing.height  / boundCircleBoxing.width;

            // Trabalhando a identificação do quadrado
            MatOfPoint2f contour2f = new MatOfPoint2f();
            MatOfPoint2f approxContour2f = new MatOfPoint2f();
            MatOfPoint approxContour = new MatOfPoint();
            contour.convertTo( contour2f, CvType.CV_32FC2);

            Double arcLenght = Imgproc.arcLength( contour2f, true );
            Imgproc.approxPolyDP( contour2f, approxContour2f, arcLenght * 0.04, true);
            approxContour2f.convertTo( approxContour, CvType.CV_32S);

            // Verifica se a forma fechada é um polígono com quatro vértices( identifica quadrado/retângulo )
            //Rect tmpRect = null;

            Mat mask = threshHold.submat( boundCircleBoxing );

            //Mat maskSquare = threshHold.submat( Imgproc.boundingRect( approxContour ) );
            //Toast.makeText(this, "preto no quadrado: " + Integer.toString( Core.countNonZero( maskSquare ) ), Toast.LENGTH_SHORT).show();

            if ( approxContour.size().height == 4 ) {
                rectRefsList.add( contour );
            }
            else {
                // Avalia se o contorno é um círculo
                if (  boundCircleBoxing.width >= minContourWidth
                        && boundCircleBoxing.height >= minContourHeight
                        && ar >= 0.82 && ar <= 1.13 )
                {
                    if ( Core.countNonZero( mask ) > ( mask.total() * 0.55 )
                            && Core.countNonZero( mask ) < ( mask.total() * 0.982 ) ) {
                        markedCirclesList.add( contour );
                    }
                }
            }



        }

        // Verifica se possui os retângulos de referência posicional
        if ( rectRefsList.size() > 1 )
        {
            // Ordena lista de larguras dos retângulos de referência para parametrizar identificador de círculos
            Collections.sort( rectRefsList, new Comparator<MatOfPoint>()
            {
                @Override
                public int compare( MatOfPoint int1, MatOfPoint int2 )
                {
                    long sumMop1 = 0;
                    long sumMop2 = 0;

                    for( Point p: int1.toList() ){
                        sumMop1 += p.x;
                    }

                    for( Point p: int2.toList() ){
                        sumMop2 += p.x;
                    }

                    if ( sumMop1 > sumMop2 ) {
                        return 1;
                    }
                    else if ( sumMop1 < sumMop2 ) {
                        return -1;
                    }
                    else {
                        return 0;
                    }

                }
            }); // fim da ordenação da lista de retângulos

            // Inicializando o Gabarito
            char[] gabarito = new char[ 45 ];

            for ( int i = 0; i < gabarito.length; i++ ) {
                gabarito[ i ] = 'x';
            }

            long grupo;
            long coluna;
            long linha;

            // Coordenadas do primeito quadrado de referência
            Rect firstSquare = Imgproc.boundingRect( rectRefsList.get( 0 ) );
            Point point0 = firstSquare.tl();
            Point point1 = firstSquare.br();
            Point pontoMedioSquareOne = new Point(  ( point0.x + point1.x ) / 2, ( point0.y + point1.y ) / 2 );

            // Coordenadas do segundo quadrado de referência
            Rect secondSquare = Imgproc.boundingRect( rectRefsList.get( 1 ) );
            Point point2 = secondSquare.tl();
            Point point3 = secondSquare.br();
            Point pontoMedioSquareTwo = new Point(  ( point2.x + point3.x ) / 2, ( point2.y + point3.y ) / 2 );

            // Coordenadas do terceiro quadrado de referência
            Rect thirdSquare = Imgproc.boundingRect( rectRefsList.get( 2 ) );
            Point point4 = thirdSquare.tl();
            Point point5 = thirdSquare.br();
            Point pontoMediosquareThree = new Point(  ( point4.x + point5.x ) / 2, ( point4.y + point5.y ) / 2 );

            // Coordenadas do quarto quadrado de referência
            Rect fourthSquare = Imgproc.boundingRect( rectRefsList.get( 3 ) );
            Point point6 = fourthSquare.tl();
            Point point7 = fourthSquare.br();
            Point pontoMedioSquareFour = new Point(  ( point6.x + point7.x ) / 2, ( point6.y + point7.y ) / 2 );

            // referências para comparar posição relativa dos círculos marcados
            Double squareOneXCordinate = pontoMedioSquareOne.x;
            Double squareOneYCordinate = pontoMedioSquareOne.y;
            Double squareTwoXCordinate = pontoMedioSquareTwo.x;
            Double squareThreeXCordinate = pontoMediosquareThree.x;
            Double squareThreeYCordinate = pontoMediosquareThree.y;
            Double squareFourXCordinate = pontoMedioSquareFour.x;
            Double squareFourYCordinate = pontoMedioSquareFour.y;

            Double initialCountX;
            Double initialCountY;
            Double colDistance;
            Double lineDistance;

            long circlesDistance =  Math.round( squareTwoXCordinate - squareOneXCordinate);

            //Toast.makeText( this, "Quantidade de alternativas marcadas: " + Integer.toString( markedCirclesList.size() ), Toast.LENGTH_SHORT ).show();

            initialCountY = squareOneYCordinate;

            ///*/ extraindo as alternativas assinaladas
            for ( MatOfPoint circle : markedCirclesList ) {

                Rect rect = Imgproc.boundingRect( circle );

                Point pointCircle1 = rect.tl();
                Point pointCircle2 = rect.br();
                Point pontoMedio = new Point(  ( pointCircle1.x + pointCircle2.x ) / 2, ( pointCircle1.y + pointCircle2.y ) / 2 );

                // identifica o grupo de alternativas, coluna e linha
                if ( pontoMedio.x < squareThreeXCordinate ) {
                    grupo = 0;

                    initialCountX = squareOneXCordinate;
                    //initialCountY = squareOneYCordinate;

                }
                else if ( pontoMedio.x >= squareThreeXCordinate
                    && pontoMedio.x < squareFourXCordinate )
                {
                    grupo = 1;

                    initialCountX = squareThreeXCordinate;
                    //initialCountY = squareThreeYCordinate;
                }
                else {
                    grupo = 2;

                    initialCountX = squareFourXCordinate;
                    //initialCountY = squareFourYCordinate;
                }


                //Toast.makeText(this, "Grupo: " + Long.toString( grupo ), Toast.LENGTH_SHORT).show();

                colDistance = pontoMedio.x - initialCountX;
                lineDistance = pontoMedio.y - initialCountY;

                // encontra a coluna da alternativa marcada
                if ( colDistance < 1 ) {
                    coluna = 1;
                }
                else {
                    if ( grupo == 0 ) {
                        coluna = Math.round( colDistance / circlesDistance ) + 1;
                    }
                    else {
                        coluna = Math.round( colDistance / circlesDistance );
                    }
                }

                // encontra a linha da alternativa marcada
                if ( lineDistance <= circlesDistance ) {
                    linha = 0;
                }
                else {
                    linha = Math.round( lineDistance / circlesDistance ) - 1;
                }

                // mapeia posição relativa do cículo em relação ao búmero da questão no gabarito
                int alternativa = ( int ) ( linha + ( grupo * 15 ) );

                //* / Extraindo o gabarito do aluno
                if ( coluna == 1 ) {
                    if ( gabarito[ alternativa ] == 'x' ) {
                        gabarito[ alternativa ] = 'a';
                    }
                    else {
                        gabarito[ alternativa ] = 'z';
                    }

                }
                else if ( coluna == 2 ) {
                    if ( gabarito[ alternativa ] == 'x' ) {
                        gabarito[ alternativa ] = 'b';
                    }
                    else {
                        gabarito[ alternativa ] = 'z';
                    }
                }
                else if ( coluna == 3 ) {
                      if ( gabarito[ alternativa ] == 'x' ) {
                         gabarito[ alternativa ] = 'c';
                      }
                      else {
                         gabarito[ alternativa ] = 'z';
                      }
                 }
                 else if ( coluna == 4 ) {
                     if ( gabarito[ alternativa ] == 'x' ) {
                         gabarito[ alternativa ] = 'd';
                     }
                     else {
                         gabarito[ alternativa ] = 'z';
                     }
                 }
                 else if ( coluna == 5 ) {
                     if ( gabarito[ alternativa ] == 'x' ) {
                        gabarito[ alternativa ] = 'e';
                     }
                     else {
                        gabarito[ alternativa ] = 'z';
                     }
                 } //*/

            }//*/ // fim da extração das alternativas assinaladas

            // mostra as 45 alternativas da prova
            StringBuilder gabaritoAluno = new StringBuilder();
            for ( int i = 0; i < gabarito.length; i++ ) {
                gabaritoAluno.append( "   " + Integer.toString( i + 1 ) + "-> " + gabarito[ i ] );
            }

            //Toast.makeText(this, "Quantidade de quadrado: " + Integer.toString( rectRefsList.size() ), Toast.LENGTH_SHORT).show();


            Toast.makeText( this, gabaritoAluno.toString(), Toast.LENGTH_LONG).show();
            Toast.makeText( this, gabaritoAluno.toString(), Toast.LENGTH_LONG).show();
            Toast.makeText( this, gabaritoAluno.toString(), Toast.LENGTH_LONG).show();
            //Toast.makeText( this, gabaritoAluno.toString(), Toast.LENGTH_LONG).show();
            //Toast.makeText( this, gabaritoAluno.toString(), Toast.LENGTH_LONG).show();
            //Toast.makeText( this, gabaritoAluno.toString(), Toast.LENGTH_LONG).show();
            //Toast.makeText( this, gabaritoAluno.toString(), Toast.LENGTH_LONG).show();
            //Toast.makeText( this, gabaritoAluno.toString(), Toast.LENGTH_LONG).show();
            //Toast.makeText( this, gabaritoAluno.toString(), Toast.LENGTH_LONG).show();
            //imageView.setTransitionName( gabaritoAluno.toString() );
        }
        else {
            Toast.makeText(this, "Documento não possui retângulos de referência", Toast.LENGTH_SHORT).show();
        }

        // Mostra resultado da imagem pré-processada
        Utils.matToBitmap( threshHold, currentBitmap );
        imageView.setImageBitmap( currentBitmap);


    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

}
