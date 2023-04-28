package com.klh.jakeinfowindow;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

public class InfoWindow {
    private Context context;
    private ViewGroup parentLayout; //infoWindowView 가 띄워질(=들어갈) 레이아웃
    private ConstraintLayout infoWindowView; //정보버튼을 누르면 띄워질 infoWindowView 인스턴스
    private String infoContent = ""; //정보창에 들어갈 내용

    private int showX = 0, showY = 0; //infoWindowView 가 parentLayout 내에 들어갈(=보여질) 최종 위치 좌표
    private String backgroundColor = "";
    private String textColor = "";
    private int radius = 20;
    private int textSize = 12;
    private int elevation = 1;

    private int correctionX = 0; //기본 gravity 에 따른 배치 후에 더 위치를 조절하고 싶을 때 사용하는 보정값
    private int correctionY = 0;

    private boolean overLayout = false; //infoWindowView 가 parentLayout 의 바깥으로 튀어나가도 되는지 여부
    private boolean animation = true; //infoWindowView 가 나타나고 사라질 때 애니메이션 실행 여부

    //infoWindowView 가 띄워지는 방식 (showMode)
    public static final int WHILE_TOUCHING = 1; //정보 버튼을 터치하는 동안에만 보여짐 (기본값)
    public static final int JUST_CLICK = 2; //정보 버튼을 누르면 보여지고 다시 누르면 사라짐
    public static final int AUTO_CLOSE = 3; //정보 버튼을 누르면 보여지고 일정 시간 후 자동으로 사라짐

    private int showMode = WHILE_TOUCHING; //infoWindowView 가 띄워지는 방식
    private int auto_close_duration = 3000; //showMode 를 AUTO_CLOSE 로 설정했을 때 infoWindowView 가 표시되는 시간

    private String gravity = "center_horizontal|top"; //infoWindowView 가 정보 버튼의 어느 쪽에 위치할지 정하는 값 (기본값: 위쪽 중앙)

    private OnInfoWindowClickListener onInfoWindowClickListener; // infoWindowView 의 클릭리스너

    private Animation showAnimation, dismissAnimation;

    public InfoWindow(Context context, ViewGroup parentLayout){ //parentLayout: infoWindowView 가 띄워질 레이아웃을 설정함. (최상위 레이아웃을 넣는 것을 권장)
        this.context = context;
        this.parentLayout = parentLayout;
        this.showAnimation = AnimationUtils.loadAnimation(context, R.anim.infowindow_show);
        this.showAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                infoWindowView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        this.dismissAnimation = AnimationUtils.loadAnimation(context, R.anim.infowindow_dismiss);
        this.dismissAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                parentLayout.removeView(infoWindowView); //뷰 제거
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    public void setOnInfoWindowClickListener(OnInfoWindowClickListener onInfoWindowClickListener){
        this.onInfoWindowClickListener = onInfoWindowClickListener;
    }

    public void setContent(String infoContent){
        this.infoContent = infoContent;
    }

    public void setBackgroundColor(String color){
        this.backgroundColor = color;
    }

    public void setTextColor(String color){
        this.textColor = color;
    }

    public void setTextSize(int size){
        this.textSize = size;
    }

    public void setRadius(int radius){
        this.radius = radius;
    }

    public void setElevation(int elevation){
        this.elevation = elevation;
    }

    public void setShowMode(int showMode){
        this.showMode = showMode;
    }

    public void setAutoCloseDuration(int duration){
        this.auto_close_duration = duration;
    }

    public void setGravity(String gravity){
        this.gravity = gravity;
    }

    public void setCorrectionX(int correctionX){
        this.correctionX = correctionX;
    }

    public void setCorrectionY(int correctionY){
        this.correctionY = correctionY;
    }

    public void canOverParentLayout(boolean overLayout){
        this.overLayout = overLayout;
    }

    public void canAnimate(boolean animation){
        this.animation = animation;
    }

    public void apply(Button infoButton){
        // infoWindowView 인스턴스 생성
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        infoWindowView = (ConstraintLayout) inflater.inflate(R.layout.info_message_view, parentLayout, false);

        if(onInfoWindowClickListener != null){
            infoWindowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onInfoWindowClickListener.onClick(infoWindowView);
                }
            });
        }

        // 설정값 적용
        TextView info_text = infoWindowView.findViewById(R.id.info_text);
        info_text.setText(infoContent);
        info_text.setTextSize(textSize);
        info_text.setElevation(elevation);

        // drawable 의 속성 변경
        GradientDrawable drawable = (GradientDrawable) ContextCompat.getDrawable(context, R.drawable.info_round);
        drawable.setCornerRadius(convertDpToPixel(radius));

        if(!backgroundColor.equals("")){
            drawable.setColor(Color.parseColor(backgroundColor));
        }
        if(!textColor.equals("")){
            info_text.setTextColor(Color.parseColor(textColor));
        }

        info_text.setBackground(drawable);

        //showMode 에 따른 조작 방법 구현
        if(showMode == InfoWindow.WHILE_TOUCHING){
            infoButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch(event.getAction()) {
                        case MotionEvent.ACTION_DOWN:{
                            show(infoButton);
                            break;
                        }
                        case MotionEvent.ACTION_UP:{
                            dismiss();
                            break;
                        }
                    }
                    return false;
                }
            });
        }else if(showMode == InfoWindow.JUST_CLICK){
            infoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    show(infoButton);
                }
            });
        }else if(showMode == InfoWindow.AUTO_CLOSE){
            infoButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch(event.getAction()) {
                        case MotionEvent.ACTION_DOWN:{
                            if (infoWindowView.getParent() == null) { // infoWindowView 의 부모가 없으면 (= 추가되지 않았다면)
                                show(infoButton);
                                new Handler().postDelayed(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        dismiss();
                                    }
                                }, auto_close_duration); //showTime 만큼 뒤에 실행
                            }
                            break;
                        }
                    }
                    return false;
                }
            });

        }

    }

    private void show(Button infoButton){
        if (infoWindowView.getParent() == null) {
            showX = infoButton.getLeft(); // infoWindowView 의 최종 좌표값을 지정함. (일단 정보 버튼 뷰의 왼쪽 끝 좌표를 더함)
            showY = infoButton.getTop(); // infoWindowView 의 최종 좌표값을 지정함. (일단 정보 버튼 뷰의 위쪽 끝 좌표를 더함)

            ViewParent viewParent = infoButton.getParent(); // 일단 비교 ViewGroup(ViewParent) 변수에 정보 버튼의 부모 레이아웃을 넣어줌

            while (viewParent != parentLayout){  // 비교대상이 parentLayout 와 다르면 (정보 버튼이 parentLayout 바로 안에 들어가 있지 않은 경우 )
                showX += ((ViewGroup)viewParent).getLeft(); // 비교 대상의 Left, Top 좌표를 최종 좌표에 더해줌
                showY += ((ViewGroup)viewParent).getTop();

                viewParent = ((ViewGroup)viewParent).getParent(); // 비교대상을 다시 설정함. 비교대상의 부모 레이아웃을 넣어줌
            } // 정보 버튼이 parentLayout 에 바로 밑에 들어가있지 않는 경우 정보 버튼의 상위 레이아웃들의 상대좌표값들을 더하는 과정임

            // 위 과정을 하는 이유는 getLeft(), getTop()이 부모레이아웃을 기준으로한 상대좌표를 반환하기 때문에
            // infoWindowView 를 parentLayout 에 넣을 때 [parentLayout 안에 있지 않은(혹은 안에 있는) 정보 버튼]의 옆에 띄우기 위해
            // 정보 버튼의 부모에 대한 상대좌표 + 정보 버튼이 속한 부모 레이아웃의 부모에 대한 상대좌표 + 정보 버튼이 속한 부모 레아이웃이 속한 부모에 대한 상대좌표 ... 를 계산하여
            // 최종적으로 parentLayout 에서의 infoWindowView 의 좌표값을 얻음

            ViewTreeObserver observer=parentLayout.getViewTreeObserver(); //infoWindowView 이 다 그려진 뒤 애니메이션을 적용하기 위해 옵저버 등록
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { //레이아웃이 다 그려지면 호출되는 리스너. 레이아웃이 다 그려지기 전에 width 나 height 값을 불러오면 0이 반환되는 걸 방지하기 위함.
                @Override
                public void onGlobalLayout() {
                    if(infoWindowView.getWidth() > 0 && infoWindowView.getHeight() > 0){
                        float gravityX = (infoWindowView.getWidth()/2)-(infoButton.getWidth()/2); //지정된 gravity 에 맞게 infoWindowView 의 위치(좌표)를 조절해주기 위한 값 (기본값: 정중앙)
                        float gravityY = (infoWindowView.getHeight()/2)-(infoButton.getHeight()/2);

                        String trimStr = gravity.replaceAll("\\p{Z}",""); //설정된 gravity 값에서 공백을 제거함
                        Log.i("인포메시지", trimStr + " trimStr");
                        String [] gravityStrList = trimStr.split("\\|"); //설정된 gravity 값을 | 로 기준으로 분리하여 배열로 받음

                        for (String gravityStr: gravityStrList) { // gravity 설정값에 맞게 gravityX, Y 값을 설정함
                            Log.i("인포메시지", gravityStr + " gravity");
                            if(gravityStr.equals("left")){
                                gravityX = infoWindowView.getWidth();
                            }else if(gravityStr.equals("right")){
                                gravityX = infoButton.getWidth();
                            }else if(gravityStr.equals("top")){
                                gravityY = infoWindowView.getHeight();
                            }else if(gravityStr.equals("bottom")){
                                gravityY = infoButton.getHeight();
                            }else if(gravityStr.equals("center_vertical")){
                                gravityY = (infoButton.getHeight()/2)-(infoWindowView.getHeight()/2);
                            }else if(gravityStr.equals("center_horizontal")){
                                gravityX = (infoButton.getWidth()/2)-(infoWindowView.getWidth()/2);
                            }else if(gravityStr.equals("center")){
                                gravityX = (infoButton.getWidth()/2)-(infoWindowView.getWidth()/2);
                                gravityY = (infoButton.getHeight()/2)-(infoWindowView.getHeight()/2);
                            }
                        }

                        infoWindowView.setX(readjustLocationX(showX-gravityX+convertDpToPixel(correctionX))); //최종(기준) 좌표에 gravity 보정값과 correctionX 보정값을 계산하여 최종 위치를 결정함
                        infoWindowView.setY(readjustLocationY(showY-gravityY+convertDpToPixel(correctionY)));


                        if(animation){
                            if(showAnimation != null){
                                infoWindowView.startAnimation(showAnimation);
                            }else{
                                infoWindowView.setVisibility(View.VISIBLE);
                            }
                        }else{
                            infoWindowView.setVisibility(View.VISIBLE);
                        }

                        if(parentLayout.getViewTreeObserver().isAlive()){ //옵저버 삭제
                            parentLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                }
            });

            parentLayout.addView(infoWindowView); //뷰 추가

            infoWindowView.setVisibility(View.INVISIBLE);
            infoWindowView.bringToFront(); //뷰 맨 앞으로 띄우기
        }
    }

    private void dismiss(){
        if(dismissAnimation != null) {
            if(animation){
                infoWindowView.startAnimation(dismissAnimation);
            }else{
                parentLayout.removeView(infoWindowView); //뷰 제거
            }
        }
    }

    private float readjustLocationX(float coordinate){ //infoWindowView 가 parentLayout 의 바깥으로 나갔다면 안쪽으로 조정한 좌표값을 반환해줌, coordinate: X 좌표
        if(overLayout){ // parentLayout 의 바깥으로 튀어나가도 된다면
            return coordinate; // 검사하지 않고 그냥 적용
        }else{ // parentLayout 의 바깥으로 튀어나가면 안 된다면 적절한 값으로 조정
            float result = coordinate;

            if(coordinate < 0){
                result = 0;
            }else if(coordinate + infoWindowView.getWidth() > parentLayout.getWidth()){
                result = parentLayout.getWidth() - infoWindowView.getWidth();
            }

            return result;
        }
    }

    private float readjustLocationY(float coordinate){ //infoWindowView 가 parentLayout 의 바깥으로 나갔다면 안쪽으로 조정한 좌표값을 반환해줌, coordinate: Y 좌표
        if(overLayout){
            return coordinate;
        }else{
            float result = coordinate;

            if(coordinate < 0){
                result = 0;
            }else if(coordinate + infoWindowView.getHeight() > parentLayout.getHeight()){
                result = parentLayout.getHeight() - infoWindowView.getHeight();
            }

            return result;
        }
    }

    private float convertDpToPixel(float dp){//Dp값을 받아 Px(픽셀) 단위로 변경된 값을 반환해줌.
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}
