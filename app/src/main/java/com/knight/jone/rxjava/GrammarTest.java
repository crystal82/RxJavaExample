package com.knight.jone.rxjava;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GrammarTest extends AppCompatActivity {


    @BindView(R.id.rlv_pic)
    RecyclerView mRlvPic;

    private OkHttpClient client;
    private List<String> mImgUrls = new ArrayList<>();
    private MyAdapter   mMyAdapter;
    private SchoolClass mSchoolClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grammar_test);
        ButterKnife.bind(this);
        client = new OkHttpClient();

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRlvPic.setLayoutManager(linearLayoutManager);
        mRlvPic.setHasFixedSize(true);

        mMyAdapter = new MyAdapter(this, mImgUrls);
        mRlvPic.setAdapter(mMyAdapter);
    }


    @OnClick({R.id.btn_clean_url, R.id.btn_text1, R.id.btn_text2, R.id.btn_text3,
            R.id.btn_text4, R.id.btn_text5, R.id.btn_text6, R.id.btn_text7})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_clean_url:
                Lg.d("----清空url缓存----");
                mImgUrls.clear();
                mMyAdapter.notifyDataSetChanged();
                break;
            case R.id.btn_text1:
                doRxText1();//基础subscriber,observer,subscribe使用
                break;

            case R.id.btn_text2:
                doRxText2();//切换线程subscribeOn,observeOn使用
                break;

            case R.id.btn_text3:
                doRxText3();//.map解析数据进行转换
                break;

            case R.id.btn_text4:
                doRxText4();//concat前一个observable执行complete之后才能执行下一个
                break;

            case R.id.btn_text5:
                //flatMap（将一个observable变为多个），
                // 实现连续请求(如注册后，需要登录|请求到学生信息后，需要继续获取学生课程)
                doRxText5();
                break;

            case R.id.btn_text6:
                // zip （将多个observable变为1个）
                // 实现多个接口数据共同更新 UI
                doRxText6();
                break;

            case R.id.btn_text7:
                doRxText7(); //interval，实现心跳间隔任务
                break;
        }
    }

    /**
     * 使用interval实现轮询，心跳任务
     * <p>
     * interval前不能加东西？
     */
    private void doRxText7() {
        final ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add("aaa");
        arrayList.add("bbb");
        arrayList.add("ccc");

        Observable
                .interval(3, 2, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {

                    private Disposable mD1;

                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        Lg.d("--------onSubscribe----");
                        mD1 = d;
                    }

                    @Override
                    public void onNext(@NonNull Long aLong) {
                        Lg.d("--------onNext----:" + aLong);
                        if (aLong >= arrayList.size()) {
                            mD1.dispose(); //停止Observable
                        } else {
                            Lg.d(arrayList.get(aLong.intValue()) + "--------onNext----:" + aLong);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Lg.d("--------onError----");
                    }

                    @Override
                    public void onComplete() {
                        Lg.d("--------onComplete----");
                    }
                });

    }

    /**
     * 使用zip,合并Observable
     */
    private void doRxText6() {
        Observable<String> observable1 = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<String> e) throws Exception {
                e.onNext("哈哈哈");
            }
        });

        Observable<Student> observable2 = Observable.create(new ObservableOnSubscribe<Student>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Student> e) throws Exception {
                e.onNext(new Student("aaa", 1));
            }
        });

        Observable.zip(observable1, observable2,
                       new BiFunction<String, Student, String>() {
                           @Override
                           public String apply(@NonNull String s, @NonNull Student student) throws Exception {
                               return s + "---" + student.name;
                           }
                       }).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                Toast.makeText(GrammarTest.this, s, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //=============================================================================
    private class SchoolClass {
        public List<Student> mStudentList;

        public SchoolClass(List<Student> studentList) {
            mStudentList = studentList;
        }
    }

    private class Student {
        public String name;
        public int    num;

        public Student(String name, int num) {
            this.name = name;
            this.num = num;
        }
    }

    /**
     * 使用FlatMap，实现连续请求。
     * flatMap 操作符可以将一个发射数据的 Observable 变换为多个 Observables ，
     * 然后将它们发射的数据合并后放到一个单独的 Observable。
     */
    private void doRxText5() {
        initSchoolClassData();
        doFlatMapTest1();//遍历SchoolClass获取数据！
        doFlatMapTest2(); //OkHttp请求获取数据
    }

    private void doFlatMapTest1() {
        Observable.fromArray(mSchoolClass)
                .flatMap(new Function<SchoolClass, ObservableSource<Student>>() {
                    @Override
                    public ObservableSource<Student> apply(@NonNull SchoolClass schoolClass) throws Exception {
                        int delayTime = (int) (1 + Math.random() * 100);
                        return Observable.fromIterable(schoolClass.mStudentList).delay(delayTime, TimeUnit
                                .MILLISECONDS);
                    }
                })
                .subscribe(new Consumer<Student>() {
                    @Override
                    public void accept(Student student) throws Exception {
                        Lg.d("student:" + student.name);
                    }
                });
    }

    private void doFlatMapTest2() {
        Observable
                .create(new ObservableOnSubscribe<Response>() {
                    @Override
                    public void subscribe(@NonNull ObservableEmitter<Response> e) throws Exception {
                        doOkhttpRequest(e);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<Response, String>() {
                    @Override
                    public String apply(@NonNull Response response) throws Exception {
                        return response.body().string();
                    }
                })
                .flatMap(new Function<String, ObservableSource<ZhuangbiPicBean>>() {
                    @Override
                    public ObservableSource<ZhuangbiPicBean> apply(@NonNull String s) throws Exception {
                        Gson gson = new Gson();
                        List<ZhuangbiPicBean> zhuangbiPicBean = gson.fromJson(s, new TypeToken<List<ZhuangbiPicBean>>
                                () {
                        }.getType());
                        return Observable.fromIterable(zhuangbiPicBean);
                    }
                })
                .subscribe(new Consumer<ZhuangbiPicBean>() {
                    @Override
                    public void accept(ZhuangbiPicBean zhuangbiPicBean) throws Exception {
                        Lg.d("ZhuangbiPicBean pic:" + zhuangbiPicBean.getImage_url());
                        mImgUrls.add(zhuangbiPicBean.getImage_url());
                        mMyAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void initSchoolClassData() {
        ArrayList<Student> students = new ArrayList();
        for (int i = 0; i < 10; i++) {
            students.add(new Student(i + "name", i));
        }
        mSchoolClass = new SchoolClass(students);
    }


    //=============================================================================

    /**
     * 采用 concat 操作符先读取缓存再通过网络请求获取数据
     */
    private void doRxText4() {
        Observable<Response> loadCache = Observable.create(new ObservableOnSubscribe<Response>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter e) throws Exception {
                Lg.d("-------concat测试1-------");
                if (mImgUrls.size() != 0) {
                    Lg.d("---加载缓存----");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mImgUrls.addAll(mImgUrls);
                            mMyAdapter.notifyDataSetChanged();
                        }
                    });
                } else {
                    Lg.d("---进行网络请求---");
                    e.onComplete();
                }
            }
        });

        Observable<Response> okHttpLoade = Observable.create(new ObservableOnSubscribe<Response>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Response> e) throws Exception {
                Lg.d("---okHttpLoade---");
                doOkhttpRequest(e);
            }
        });

        Observable.concat(loadCache, okHttpLoade)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<Response, String>() {
                    @Override
                    public String apply(@NonNull Response response) throws Exception {
                        return response.body().string();
                    }
                })
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String string) throws Exception {
                        Gson gson = new Gson();
                        List<ZhuangbiPicBean> zhuangbiPicBean =
                                gson.fromJson(string, new TypeToken<List<ZhuangbiPicBean>>() {
                                }.getType());
                        for (ZhuangbiPicBean bean : zhuangbiPicBean) {
                            Lg.d("----zhuangbi" + bean.getImage_url() + "----");
                            mImgUrls.add(bean.getImage_url());
                        }
                        mMyAdapter.notifyDataSetChanged();
                    }
                });
    }

    /**
     * map的使用，结合OkHttp
     * <p>
     * 1、被观察者执行操作subscribe中返回内容作为Map的key(observable.next(内容))
     * 2、Observable.map(new Function<next传入内容，解析内容返回>){}
     * 3、Observable.doOnNext()得到解析返回内容
     * <p>
     * ????  map前面执行！！就出现异常
     * Log.d("asd", "------map-----:"+response.body().string());
     */
    private void doRxText3() {
        Observable.create(new ObservableOnSubscribe<Response>() {
            @Override
            public void subscribe(@NonNull final ObservableEmitter<Response> observableEmitter) throws Exception {
                Log.d("asd", "------subscribe-----");
                doOkhttpRequest(observableEmitter);
            }
        }).map(new Function<Response, String>() {//将一个observable转化为另外一个
            @Override
            public String apply(@NonNull Response response) throws Exception {
                return response.body().string();
            }
        }).subscribeOn(Schedulers.io()) //设置被观察者执行线程
                .observeOn(AndroidSchedulers.mainThread()) //设置观察者执行线程
                .doOnNext(new Consumer<String>() {//相应observable.next()方法,接收map数据
                    @Override
                    public void accept(String s) throws Exception {
                        //相当于Observe中的next
                        //Lg.d("----doOnNext：" + s + "---执行操作-");
                    }
                })
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        //Lg.d("----成功：" + s + "----");
                        //Gson解析数据
                        Gson gson = new Gson();
                        List<ZhuangbiPicBean> zhuangbiPicBean = gson.fromJson(s, new TypeToken<List<ZhuangbiPicBean>>
                                () {
                        }.getType());
                        for (ZhuangbiPicBean bean : zhuangbiPicBean) {
                            Lg.d("----zhuangbi" + bean.getImage_url() + "----");
                            mImgUrls.add(bean.getImage_url());
                        }
                        mMyAdapter.notifyDataSetChanged();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Lg.d("----失败：" + throwable.toString() + "----");
                    }
                });
    }

    private void doOkhttpRequest(@NonNull final ObservableEmitter<Response> observableEmitter) {
        Request.Builder builder = new Request.Builder();
        Request         request = builder.url("https://www.zhuangbi.info/search?q=%E5%8F%AF%E7%88%B1").build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                observableEmitter.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    observableEmitter.onNext(response);
                }
                //observableEmitter.onComplete();//访问结束
            }
        });
    }

    /**
     * 测试线程切换
     * 1、subscribeOn --- subscribe被观察者的线程(事件发生地)
     * 2、observerOn --- observe观察者线程（事件处理地）
     * <p>
     * Schedulers.io() 代表io操作的线程, 通常用于网络,读写文件等io密集型的操作；
     * Schedulers.computation() 代表CPU计算密集型的操作, 例如需要大量计算的操作；
     * Schedulers.newThread() 代表一个常规的新线程；
     * AndroidSchedulers.mainThread() 代表Android的主线程
     */
    private void doRxText2() {
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<String> e) throws Exception {
                Lg.d("---RxJava2---subscribe" + Thread.currentThread() + "----");
                e.onNext("测试线程");
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        Lg.d("----RxJava2----onSubscribe:" + Thread.currentThread());
                    }

                    @Override
                    public void onNext(@NonNull String s) {
                        Lg.d("----RxJava2----onNext:" + Thread.currentThread());
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Lg.d("----RxJava2----onError:" + Thread.currentThread());
                    }

                    @Override
                    public void onComplete() {
                        Lg.d("----RxJava2----onComplete:" + Thread.currentThread());
                    }
                });
    }

    /**
     * RxJava测试1：简单创建
     * 1.Observable.create 创建被观察者
     * 2.创建观察者Observer，处理被观察者返回结果
     * 3.subscribe(Observer)使用“观察者”进行订阅，建立关系
     */
    private void doRxText1() {
        //第一步，初始化被观察着
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<String> e) throws Exception {
                Lg.d("----subscribe" + e.toString() + "----");
                e.onNext("哈哈哈");
                e.onNext("2222");
                //e.onError(new Throwable("出现错误")); //执行后自动停止观察
                //e.onComplete(); //执行后自动停止观察
                e.onNext("stop");//执行 disposable.停止执行
                e.onNext("收不到了");
            }
        }).subscribe(//第三步，订阅建立关系
                     //第二步，初始化观察者 ---->处理被观察者返回内容
                     new Observer<String>() {
                         private Disposable mDisposable;

                         @Override
                         public void onSubscribe(@NonNull Disposable d) {
                             Lg.d("----RxJava----onSubscribe: 开始前准备" + d.toString());
                             mDisposable = d; //用于停止订阅
                         }

                         @Override
                         public void onNext(@NonNull String s) {
                             Lg.d("----RxJava----onNext：" + s);
                             if ("stop".equals(s)) {
                                 mDisposable.dispose();
                             }
                         }

                         @Override
                         public void onError(@NonNull Throwable e) {
                             Lg.d("----RxJava----onError:" + e.toString());
                         }

                         @Override
                         public void onComplete() {
                             Lg.d("----RxJava----onComplete");
                         }
                     });

    }


    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

        private final List<String> mImgUrls;
        private final Context      mContext;

        public MyAdapter(Context context, List<String> imgUrls) {
            mContext = context;
            mImgUrls = imgUrls;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_img_item, parent, false);
            return new MyViewHolder(inflate);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            Picasso.with(mContext)
                    .load(mImgUrls.get(position))
                    .into(holder.mImageView);
        }

        @Override
        public int getItemCount() {
            return mImgUrls.size();
        }

        //自定义的ViewHolder，持有每个Item的的所有界面元素
        public class MyViewHolder extends RecyclerView.ViewHolder {
            public ImageView mImageView;

            public MyViewHolder(View view) {
                super(view);
                mImageView = (ImageView) view.findViewById(R.id.iv_url_load);
            }
        }
    }
}
