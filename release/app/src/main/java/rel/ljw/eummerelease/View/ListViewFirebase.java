package rel.ljw.eummerelease.View;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rel.ljw.eummerelease.Model.Constants;
import rel.ljw.eummerelease.Model.DBHelper;
import rel.ljw.eummerelease.Model.RecordingMataData;
import rel.ljw.eummerelease.Model.memoItem;
import rel.ljw.eummerelease.Presenter.ListViewAdapter;
import rel.ljw.eummerelease.R;


public class ListViewFirebase extends AppCompatActivity {
    String tag = "myListviewfirebase";

    public ListViewAdapter adapterFB = null;
    public android.widget.ListView listviewFB = null;

    FirebaseStorage storage = FirebaseStorage.getInstance();
    public ProgressDialog progressDialog;


    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference = firebaseDatabase.getReference();

    List list;

    List memolist = new ArrayList();
    List memoindexlist = new ArrayList();
    List memotimelist = new ArrayList();

    DBHelper mDBHelper;

    List<RecordingMataData> metaList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_view);
        Intent intent = getIntent();
        HashMap<String, List<String>> set = (HashMap<String, List<String>>) intent.getSerializableExtra("set");
        Log.d(tag, "size: " + set.size());

        list = new ArrayList();     // 업로드된 파일 이름 담는 배열

        mDBHelper = new DBHelper(this);             /////////////////
        mDBHelper.open();

        adapterFB = new ListViewAdapter();
        listviewFB = findViewById(R.id.listview1);
        if(listviewFB ==null){
            Log.d(tag, "listviewFB is null");
        }

        listviewFB.setAdapter(adapterFB);

        for( String key : set.keySet() ){
            Log.d(tag, "key : "+ key);
            for(String value : set.get(key)){
                Log.d(tag, "values : " + value);
            }
        }

        for(String fileName : set.keySet()){
            String t = fileName;
            list.add(fileName);
            List<String> temp = new ArrayList<>();
            for(String value : set.get(fileName)){
                temp.add(value);
            }
            adapterFB.addItem(ContextCompat.getDrawable(this,R.drawable.play_button),
                    t,
                    temp.get(1),
                    temp.get(0),
                    true,
                    false);
        }

        listviewFB.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            //ListView의 아이템 중 하나가 클릭될 때 호출되는 메소드
            //첫번째 파라미터 : 클릭된 아이템을 보여주고 있는 AdapterView 객체(여기서는 ListView객체)
            //두번째 파라미터 : 클릭된 아이템 뷰
            //세번째 파라미터 : 클릭된 아이템의 위치(ListView이 첫번째 아이템(가장위쪽)부터 차례대로 0,1,2,3.....)
            //네번재 파리미터 : 클릭된 아이템의 아이디(특별한 설정이 없다면 세번째 파라이터인 position과 같은 값)
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            }
        });

        final EditText editSearch = findViewById(R.id.editSearch);
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                String text = editSearch.getText().toString();
                ((ListViewAdapter)listviewFB.getAdapter()).getFilter().filter(text);
            }

        });

        registerForContextMenu(listviewFB);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(tag, "resuem Resume");

        // 파베에서 데이터 읽어서 웹에 있는애들 가져옴.
        databaseReference.child(Constants.getUserUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(tag, "파일 개수 : " + dataSnapshot.getChildrenCount());
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Log.d(tag, "file name : " + ds.getKey());                    // 업로드한 파일이름 뽑아오기

                    List<String> memotemp = new ArrayList();
                    List<String> memoindextemp = new ArrayList();
                    List<String> memotimetemp = new ArrayList();

                    for(DataSnapshot snapbaby : ds.getChildren()){

                        if(snapbaby.getKey().equals("createdTime")){
                            Log.d(tag, "createdtime 추가 : "+ snapbaby.getValue());
                        }
                        if(snapbaby.getKey().equals("playTime")){
                            Log.d(tag, "playtime 추가 : "+ snapbaby.getValue());
                        }
                        if(snapbaby.getKey().equals("memo")){
                            for(DataSnapshot baby : snapbaby.getChildren()){
                                memotemp.add(baby.getValue().toString());
                                Log.d(tag, "메모 추가 : " + baby.getValue());
                            }
                        }
                        if(snapbaby.getKey().equals("memoIndex")){
                            for(DataSnapshot baby : snapbaby.getChildren()){
                                if(baby.getValue().toString().equals("")) {
                                    memoindextemp.add("0");
                                }
                                else{
                                    memoindextemp.add(baby.getValue().toString());
                                }
                                Log.d(tag, "메모인덱스 추가 : " + baby.getValue());
                            }
                        }
                        if(snapbaby.getKey().equals("memoTime")){
                            for(DataSnapshot baby : snapbaby.getChildren()){
                                memotimetemp.add(baby.getValue().toString());
                                Log.d(tag, "메모타임 추가 : " + baby.getValue());
                            }
                        }
                    }

                    List<memoItem> memoitemlisttemp = new ArrayList<>();
                    int len = memotemp.size();

                    for(int i=0;i<len; i++){
                        memoItem itemtemp = new memoItem(memotemp.get(i), memotimetemp.get(i), Integer.parseInt(memoindextemp.get(i)));
                        memoitemlisttemp.add(itemtemp);
                    }

                    RecordingMataData temp = new RecordingMataData(ds.getKey().toString(), memoitemlisttemp);
                    metaList.add(new RecordingMataData(ds.getKey(), memoitemlisttemp));
                    Log.d(tag, "getvalue : " + ds.getValue());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }


    //Context 메뉴로 등록한 View(여기서는 ListView)가 처음 클릭되어 만들어질 때 호출되는 메소드
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        //res폴더의 menu플더안에 xml로 MenuItem추가하기.
        //mainmenu.xml 파일을 java 객체로 인플레이트(inflate)해서 menu객체에 추가
        getMenuInflater().inflate(R.menu.recording_file_context_menu_fb, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }
    //    //Context 메뉴로 등록한 View(여기서는 ListView)가 클릭되었을 때 자동으로 호출되는 메소드
    /// 각 아이템 클릭했을떄 어떻게 처리할지
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        //AdapterContextMenuInfo
        //AdapterView가 onCreateContextMenu할때의 추가적인 menu 정보를 관리하는 클래스
        //ContextMenu로 등록된 AdapterView(여기서는 Listview)의 선택된 항목에 대한 정보를 관리하는 클래스
        AdapterView.AdapterContextMenuInfo info= (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        int index= info.position; //AdapterView안에서 ContextMenu를 보여주는 항목의 위치
//        String fileName = myList.get(index).toString();
        //예제에서는 선택된 ListView의 항목(String 문자열) data와 해당 메뉴이름을 출력함
        switch( item.getItemId() ){
            case R.id.download:
                Toast.makeText(getApplicationContext(), "download", Toast.LENGTH_SHORT).show();
                String filename = list.get(index).toString();
                downLoad(filename, index);
                insertToDB(filename);
                /////////
                break;
            case R.id.delete:
                Toast.makeText(getApplicationContext(), "delete", Toast.LENGTH_SHORT).show();
                delete(list.get(index).toString(),index);
                break;
        }
        return true;
    }


    public void downLoad(String fileName, int idx){
        //TODO 파베에 있는 파일들 중 이름에 빈칸이있으면 다운이 안댐.

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("데려오는중...");
        progressDialog.show();
        progressDialog.onStart();
        Log.d(tag, "다운받을 파일 : " + fileName);
        StorageReference storageRef = storage.getReference();
        StorageReference islandRef = storageRef.child("users/" + Constants.getUserUid() + "/Recording/" + fileName+".mp4");
        Log.d(tag, "파베에 파일 위치 : " + "users/" + Constants.getUserUid() + "/Recording/" + fileName+".mp4");
        File localFile=null;
        try{
            localFile = File.createTempFile("temp", ".mp4", Environment.getExternalStorageDirectory());

        } catch(IOException e){
            e.printStackTrace();
        }


        final File finalLocalFile = localFile;
        final String finalFileName = fileName;
        final int finalIdx = idx;
        islandRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                // Local temp file has been created
                Log.d(tag, "다운로드 완료");
                Log.d(tag, "파일 위치 : " + finalLocalFile.getPath());
                moveFile(finalLocalFile, finalFileName);
                adapterFB.modifyIsDownloded(finalIdx, true);

                adapterFB.notifyDataSetChanged();
                progressDialog.dismiss();

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(tag, "다운로드 실패");
                progressDialog.dismiss();
                // Handle any errors
            }
        }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d(tag, "on progress");
                double progress = (100.0 * taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount();
                //dialog에 진행률을 퍼센트로 출력해 준다
                progressDialog.setMessage("Downloaded " + ((int)progress) + "% ...");
            }
        });
        localFile.delete();
    }

    //////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////
    public void moveFile(File from, String name) {
//        File file = new File("D:\\Test.java");
        File file = from;
        File file2 = new File(Constants.getFilePath()+"/"+name+".mp4");//이동

        if(file.exists()) {
            Log.d(tag, "이동 완료");
            file.renameTo(file2);	//변경

        }
    }

    public void delete(final String fileName, final int idx){
        StorageReference storageRef = storage.getReference();
        StorageReference desertRef = storageRef.child("users/" + Constants.getUserUid() + "/Recording/" + fileName+".mp4");
        Log.d(tag, "delete : " + fileName);

        desertRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(getApplicationContext(), "삭제 완료", Toast.LENGTH_SHORT).show();
                Log.d(tag, "삭제완료");
                list.remove(idx);
                adapterFB.deleteItem(idx);
                adapterFB.notifyDataSetChanged();
                deleteRealtimeDB(fileName);

                // File deleted successfully
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(getApplicationContext(), "삭제 실패", Toast.LENGTH_SHORT).show();
                Log.d(tag, "삭제실패");

                // Uh-oh, an error occurred!
            }
        });
    }

    public void deleteRealtimeDB(String fileName){

        Log.d(tag, "delete in db  : " + fileName);
        databaseReference.child(Constants.getUserUid()).child(fileName).setValue(null);

    }

    public void insertToDB(String fileName){
        Log.d(tag, "insert to db : " + fileName);
        int len = metaList.size();
        for(int i=0;i<len; i++){
            Log.d(tag, "filename test : " + metaList.get(i).getFileName());
            if(metaList.get(i).getFileName().equals(fileName.toString())){
                Log.d(tag, "file name : " + fileName);
                List<memoItem> temp = metaList.get(i).getMemoItemList();
                int len2 = temp.size();
                Log.d(tag, "len 2 : " + len2);
                for(int j=0;j<len2;j++){
                    Log.d(tag, "memo : " + temp.get(j).getMemo());
                    Log.d(tag, "time : " + temp.get(j).getMemoTime());
                    Log.d(tag, "index : " + temp.get(j).getMemoIndex());
                    mDBHelper.insert(fileName+".mp4", temp.get(j).getMemo(), temp.get(j).getMemoTime(), temp.get(j).getMemoIndex());
                }
            }
        }
    }



}