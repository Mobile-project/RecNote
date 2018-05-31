package com.sample.andremion.musicplayer.View;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.sample.andremion.musicplayer.Model.Constants;
import com.sample.andremion.musicplayer.Presenter.ListViewAdapter;
import com.sample.andremion.musicplayer.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ListView extends AppCompatActivity{
    private File file;
    private List myList;
    private List myListDate;
    String tag = "myListViewActivity";

    //////////////////////////////////////////////////////////////
    FirebaseStorage storage = FirebaseStorage.getInstance();

    // Create a storage reference from our app
    //참조를 만들려면 FirebaseStorage 싱글톤 인스턴스를
    // 사용하고 이 인스턴스의 getReference() 메소드를 호출합니다.
    StorageReference storageRef = storage.getReference();
    private Uri filePath;

    public ProgressDialog progressDialog;
    public int index=0;

    public ListViewAdapter adapter;
    public android.widget.ListView listview ;

    //////////////////////////////////////////////////////////////

    String newName="";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_view);

        myList = new ArrayList();
        myListDate = new ArrayList();
        String rootSD = Environment.getExternalStorageDirectory().toString();
        rootSD+="/ZEum_me";
        Log.d(tag,rootSD);
        Log.d(tag,"after rootSD");

        file = new File(rootSD);
        File list[] = file.listFiles();
        Log.d(tag,"after file list[] : " + list.length);


        // 파일 이름들 추가
        for(int i=0;i<list.length;i++){
            myList.add(list[i].getName());
            myListDate.add(list[i].lastModified());
        }
        Log.d(tag,"after myList add");




        // Adapter 생성
        adapter = new ListViewAdapter() ;

        // 리스트뷰 참조 및 Adapter달기
        listview = (android.widget.ListView) findViewById(R.id.listview1);
        listview.setAdapter(adapter);

        // 아이템 추가
        for(int i=0;i<myList.size();i++){
            Log.d(tag, "rootSD : " + rootSD);
            String filename = myList.get(i).toString();
            adapter.addItem(ContextCompat.getDrawable(this,R.drawable.btn_play),
                    filename,
                    getPlayTime(rootSD+"/"+filename),
                    getCreatedTime(Long.valueOf(myListDate.get(i).toString())));
        }

        // 위에서 생성한 listview에 클릭 이벤트 핸들러 정의.
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            //ListView의 아이템 중 하나가 클릭될 때 호출되는 메소드
            //첫번째 파라미터 : 클릭된 아이템을 보여주고 있는 AdapterView 객체(여기서는 ListView객체)
            //두번째 파라미터 : 클릭된 아이템 뷰
            //세번째 파라미터 : 클릭된 아이템의 위치(ListView이 첫번째 아이템(가장위쪽)부터 차례대로 0,1,2,3.....)
            //네번재 파리미터 : 클릭된 아이템의 아이디(특별한 설정이 없다면 세번째 파라이터인 position과 같은 값)
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // TODO Auto-generated method stub
                //클릭된 아이템의 위치를 이용하여 데이터인 문자열을 Toast로 출력
                Toast.makeText(getApplicationContext(), myList.get(position).toString(),Toast.LENGTH_SHORT).show();

            }

        });

//        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView parent, View v, int position, long id) {
//                // get item
//                ListViewItem item = (ListViewItem) parent.getItemAtPosition(position) ;
//                Log.d(tag, "item click listener");
//                Toast.makeText(getApplicationContext(), "item num : " + position, Toast.LENGTH_SHORT).show();
//
//                // TODO : use item data.
//            }
//        }) ;

        listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getApplicationContext(), "longlong : " + myList.get(position).toString(), Toast.LENGTH_SHORT).show();

                Log.d(tag, "item long click listener");
//                upLoad(myList.get(position).toString());          // 업로드
                return false;
            }
        });

        registerForContextMenu(listview);

    }


    // path에 있는 파일 길이 가져오기.
    // hh:mm:ss
    private String getPlayTime(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long timeInmillisec = Long.parseLong( time );
        long duration = timeInmillisec / 1000;
        long hours = duration / 3600;
        long minutes = (duration - hours * 3600) / 60;
        long seconds = duration - (hours * 3600 + minutes * 60);
        return hours + ":" + minutes + ":" + seconds;
    }

    /**
     *
     * @param date
     * @return
     * yyyy/MM/dd 형식으로 date 변환
     *
     */
    private String getCreatedTime(long date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        return sdf.format(date).toString();
    }


    //////////////////////////////////////////////////////////////
    ///////////////////////FIRE BASE UPLOAD///////////////////////
    //////////////////////////////////////////////////////////////
    public void upLoad(String fileName) {
        UploadTask uploadTask;
        filePath = Uri.parse(Constants.getFilePath() + "/" + fileName);                                 // 올라갈 파일 경로
        Log.d(tag, "filePath : " + filePath);
        FirebaseStorage storage = FirebaseStorage.getInstance("gs://eumme-c2ce7.appspot.com/");         // 올릴 저장소 주소
        //위에서 생성한 FirebaseStorage 를 참조하는 storage를 생성한다
        StorageReference storageRef = storage.getReference();                                           // 저장소에 대한 레퍼런스

        // 위의 저장소를 참조하는 images폴더안의 space.jpg 파일명으로 지정하여
        // 하위 위치를 가리키는 참조를 만든다
//        StorageReference spaceRef = storageRef.child("images/space.jpg");                               // 무엇?

        Uri file = Uri.fromFile(new File(Constants.getFilePath() + "/" + fileName));            // 올라갈 파일을 객체로 가져옴
        StorageReference Ref = storageRef.child("Recording/" + file.getLastPathSegment());            //
        uploadTask = Ref.putFile(file);                                                                 // 파일 올리는 태스크에 파일 장착

        // 상태바표시
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("uploading...");
        progressDialog.show();
        progressDialog.onStart();
        Log.d(tag, "after uploading...");


        // 파일 업로드의 성공/실패에 대한 콜백 받아 핸들링 하기 위해 아래와 같이 작성한다
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(tag, "on Failure");
                // Handle unsuccessful uploads
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), "업로드 실패!", Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(tag, "onSuccess");
                progressDialog.dismiss(); //업로드 진행 Dialog 상자 닫기
                Toast.makeText(getApplicationContext(), "업로드 완료!", Toast.LENGTH_SHORT).show();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(tag, "onProgressss");
                //이걸 넣어 줘야 아랫줄에 에러가 사라진다. 넌 누구냐?
//                @SuppressWarnings("VisibleForTests")
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                //dialog에 진행률을 퍼센트로 출력해 준다
                progressDialog.setMessage("Uploaded " + ((int) progress) + "% ...");
            }
        });
    }
    //////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////


    //Context 메뉴로 등록한 View(여기서는 ListView)가 처음 클릭되어 만들어질 때 호출되는 메소드
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        // TODO Auto-generated method stub
        //res폴더의 menu플더안에 xml로 MenuItem추가하기.
        //mainmenu.xml 파일을 java 객체로 인플레이트(inflate)해서 menu객체에 추가
        getMenuInflater().inflate(R.menu.recording_file_context_menu, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }


//    //Context 메뉴로 등록한 View(여기서는 ListView)가 클릭되었을 때 자동으로 호출되는 메소드
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        //AdapterContextMenuInfo
        //AdapterView가 onCreateContextMenu할때의 추가적인 menu 정보를 관리하는 클래스
        //ContextMenu로 등록된 AdapterView(여기서는 Listview)의 선택된 항목에 대한 정보를 관리하는 클래스
        AdapterView.AdapterContextMenuInfo info= (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        index= info.position; //AdapterView안에서 ContextMenu를 보여즈는 항목의 위치
        //선택된 ContextMenu의  아이템아이디를 구별하여 원하는 작업 수행
        //예제에서는 선택된 ListView의 항목(String 문자열) data와 해당 메뉴이름을 출력함
        switch( item.getItemId() ){
            case R.id.changeName:
                Toast.makeText(this, myList.get(index)+" Change Name", Toast.LENGTH_SHORT).show();
                // 이름 새로 받아야함
//                ((MainActivity)MainActivity.mContext).dbHelper.noName(myList.get(index).toString(), newName);
                setNewName("");
                customDialog(index);
                if(!newName.equals("")){
                    Log.d(tag, "이프문에 들어왔다.");
                    nameChange(myList.get(index).toString(), newName);
                    ((MainActivity)MainActivity.mContext).dbHelper.reName(myList.get(index).toString(), newName);

                    myList.remove(index);
                    adapter.notifyDataSetChanged();

                } else{
                    Log.d(tag, "엘스문이다.");
                }

                // 파일 이름 바꾸는 함수
                break;
            case R.id.upload:
                Toast.makeText(this, myList.get(index)+" Upload", Toast.LENGTH_SHORT).show();
                upLoad(myList.get(index).toString());
                break;
            case R.id.delete:
                Toast.makeText(this, myList.get(index)+" Delete", Toast.LENGTH_SHORT).show();
                ((MainActivity)MainActivity.mContext).dbHelper.delete(myList.get(index).toString());
                File file = new File(Constants.getFilePath()+"/"+myList.get(index).toString());
                Log.d(tag, "delete : " + Constants.getFilePath()+"/"+myList.get(index).toString());
                file.delete();


                /////////////////////////
                // 바로 갱신 안댐 //////////
//                runOnUiThread(new Runnable() {
//                    public void run() {
//                        //reload content
//
//                        myList.remove(index);
//                        adapter.notifyDataSetChanged();
//                        listview.invalidateViews();
//                        listview.refreshDrawableState();
//                    }
//                });
                /////////////////////////
                /////////////////////////


                break;
        }
        return true;
    };


//    //ListView의 아이템 하나가 클릭되는 것을 감지하는 Listener객체 생성 (Button의 OnClickListener와 같은 역할)
//    AdapterView.OnItemClickListener listener= new AdapterView.OnItemClickListener() {
//        //ListView의 아이템 중 하나가 클릭될 때 호출되는 메소드
//        //첫번째 파라미터 : 클릭된 아이템을 보여주고 있는 AdapterView 객체(여기서는 ListView객체)
//        //두번째 파라미터 : 클릭된 아이템 뷰
//        //세번째 파라미터 : 클릭된 아이템의 위치(ListView이 첫번째 아이템(가장위쪽)부터 차례대로 0,1,2,3.....)
//        //네번재 파리미터 : 클릭된 아이템의 아이디(특별한 설정이 없다면 세번째 파라이터인 position과 같은 값)
//        @Override
//        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//
//            // TODO Auto-generated method stub
//            //클릭된 아이템의 위치를 이용하여 데이터인 문자열을 Toast로 출력
//            Toast.makeText(getApplicationContext(), myList.get(position).toString(),Toast.LENGTH_SHORT).show();
//
//        }
//
//    };





    public void nameChange(String preName, String newName){
        Log.d(tag, "in name change : " +Constants.getFilePath()+"/"+ preName);
        Log.d(tag, "in name change : " + Constants.getFilePath()+"/" +newName+".mp4");
        File filePre = new File(Constants.getFilePath()+"/", preName);
        File fileNow = new File(Constants.getFilePath()+"/", newName+".mp4");

        if(filePre.renameTo(fileNow)){
            Toast.makeText(getApplicationContext(), "변경 성공", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getApplicationContext(), "변경 실패", Toast.LENGTH_SHORT).show();
        }
    }



    public void customDialog(final int index){
        final String[] ret = {""};
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Input your name");
        alert.setMessage("Plz, input yourname");


        final EditText name = new EditText(this);
        alert.setView(name);

        alert.setNegativeButton("Cancle",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });

        alert.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                ret[0] = name.getText().toString();
                Log.d(tag, "셋 전 : " + ret[0]);
                setNewName(ret[0]);
                nameChange(myList.get(index).toString(), ret[0]);
            }
        });

        alert.show();

    }

    public void setNewName(String t){
        this.newName = t;
    }

    public String getNewName(String t){
        return this.newName;
    }

}