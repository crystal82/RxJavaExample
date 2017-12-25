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
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.CompletableSource;
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

/**
 * @author hwq
 *         RxJava2.0 入门指导
 *         1、使用机制
 *         2、相关操作符
 */
public class Rx2Tutorial1 extends AppCompatActivity {


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
            R.id.btn_text4, R.id.btn_text5, R.id.btn_text6, R.id.btn_text7, R.id.btn_text8, R.id.btn_text9})
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
                // zip （分别从发射器A和发射器B各取出一个事件来组合，并且一个事件只能被使用一次，组合的顺序是严格按照事件发送的顺序来进行的）
                // 组合一一对应，多余的不要
                // 最终接收器收到的事件数量是和发送器发送事件最少的那个发送器的发送事件数目相同
                doRxText6();
                break;

            case R.id.btn_text7:
                doRxText7(); //interval，实现心跳间隔任务
                intervalCountdown(10)
                        .subscribe(new Consumer<Long>() {
                            @Override
                            public void accept(Long aLong) throws Exception {
                                Lg.d("Interval ContDown 倒计时:" + aLong);
                            }
                        });
                break;

            case R.id.btn_text8:
                doRxText8();
                break;
            case R.id.btn_text9:
                doRxText9(); //BackPressure
                break;
            default:
                break;
        }
    }

    /**
     * BackPressure
     * 被观察者发送消息太快以至于它的操作符或者订阅者不能及时处理相关的消息
     * <p>
     * Observable：不支持 backpressure 处理，所有待处理数据存在内存中，等待处理
     * 不会发生 MissingBackpressureException 异常。
     * 坏处是：当产生的数据过快，内存中缓存的数据越来越多，占用大量内存。
     * <p>
     * Flowable：支持处理背压问题，最多缓存128个数据，超过出现MissingBackpressureException
     * （onBackpressureDrop 一定要放在 interval 后面否则不会生效）
     * 1、onBackpressureDrop()：Drop 就是直接把存不下的事件丢弃
     * 2、onBackpressureLatest：就是只保留最新的事件。
     * 2、onBackpressureBuffer（int 缓存队列大小）：缓存所有的数据，不会丢弃数据。(超过崩溃)
     */
    private void doRxText9() {
        Lg.d("BackPressureTest:doRxText9");
        //MissingBackpressureException异常演示，Flowable只支持128个线程
        //doBackpressError();
    }

    private void doBackpressError() {
        Flowable.interval(1, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        Thread.sleep(1000);
                        Lg.d("BackPressureTest:" + aLong);
                    }
                });
    }

    /**
     * 0.take:执行指定次数！！！
     * 1.just(T1,T2...,Tn):可传入数组，或集合，发射N个Observable,依次处理
     * 2.repeat(int):重复指定次数，不输入无限轮询
     * 3.range( int start , int end ) :发送特定整数序列的Observable  start-->end
     * 4.fromArray(T[]):遍历数组
     * 5.fromIterator(Iterator ):遍历集合
     * 6.toList():把数据变成List集合
     * 7.delay(long，TimeUnit):延迟指定时间开始
     * 8.Observable.empty/never/error:正常终止的Observable
     * ****8.1、empty:不发射任何数据但是正常终止的Observable
     * ****8.2、never:不发射数据也不终止的Observable
     * ****8.3、error:"不发射数据以一个错误终止的Observable
     * 9.timer/interval、intervalRange/delay
     * ****9.1、timer：创建一个Observable，并延迟发送一次的操作符
     * ****9.2、interval：按固定时间间隔发射整数序列的Observable
     * (intervalRange(开始，结束，第一个延迟，间隔，schedule线程))
     * ****9.3、delay：不创建，用于事件流中，可以延迟发送事件流中的某一次发送。
     * Observable.timer(5, TimeUnit.SECONDS) = Observable.fromIterable(list).delay(5, TimeUnit.MILLISECONDS);
     * <p>
     * 10.defer/fromCallable
     * ****defer:使用defer延迟执行，直到被订阅才执行"just"
     * ****fromCallable:
     */
    private void doRxText8() {
        Lg.d("---doRxText8---");
        //doJustTest();
        //doRepeatTest();
        //doRangeTest();
        //doFromArrayTest();
        //doFromIterableTest();
        //doDelayTest();
        //doDeferTest();

        Observable.fromCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "sadsd";
            }
        }).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {

            }
        });
    }

    private void doDeferTest() {
        SomeType           instance = new SomeType();
        Observable<String> value    = instance.valueCreateObservable();
        instance.setValue("Some Value222");
        value.subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                Lg.d("----------:" + s);
            }
        });
    }

    public class SomeType {
        private String value;

        public void setValue(String value) {
            this.value = value;
        }

        //证明：just()，from()这类能够创建Observable的操作符在创建之初，就已经存储了对象的值
        //just会马上执行！！！出现空指针异常~
        public Observable<String> valueObservableError() {
            return Observable.just(value);
        }

        //使用defer延迟执行，直到被订阅才执行"just"
        public Observable<String> valueDeferObservable() {
            return Observable.defer(new Callable<ObservableSource<? extends String>>() {
                @Override
                public ObservableSource<? extends String> call() throws Exception {
                    return Observable.just(value);
                }
            });
        }

        //使用defer延迟执行，直到被订阅才执行"just"
        public Observable<SomeType> valueDeferObservable2() {
            return Observable.defer(new Callable<Observable<SomeType>>() {
                @Override
                public Observable<SomeType> call() throws Exception {
                    SomeType someType = new SomeType();
                    someType.setValue(value);

                    //执行一个复杂操作
                    //try {
                    //    db.writeToDisk(someType);
                    //} catch (IOException e) {
                    //    return Observable.error(e);
                    //}

                    return Observable.just(someType);
                }
            });
        }

        //使用create方法，它允许订阅者控制事件的发送
        public Observable<String> valueCreateObservable() {
            return Observable.create(new ObservableOnSubscribe<String>() {
                @Override
                public void subscribe(@NonNull ObservableEmitter<String> e) throws Exception {
                    e.onNext(value);
                    e.onComplete();
                }
            });
        }
    }

    private void doDelayTest() {
        Observable.fromArray(1, 2, 3)
                .delay(3, TimeUnit.SECONDS)
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Lg.d("delay:" + integer);
                    }
                });
    }

    private void doFromIterableTest() {
        ArrayList<Integer> objects = new ArrayList<>();
        objects.add(1);
        objects.add(2);
        objects.add(3);
        Observable
                .fromIterable(objects)
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Lg.d("fromIterable:" + integer);
                    }
                });
    }

    private void doFromArrayTest() {
        Integer[] items = {0, 1, 2, 3};
        Observable
                .fromArray(items)
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Lg.d("fromArray: " + integer);

                    }
                });
    }

    private void doRangeTest() {
        Observable.range(1, 5)
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Lg.d("range:" + integer);
                        //range:1
                        //range:2
                        //range:3
                        //range:4
                        //range:5
                    }
                });
    }


    private void doRepeatTest() {
        Observable.just(1, 2)
                .repeat(3)
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Lg.d("repeat:" + integer);
                        //repeat:1
                        //repeat:2
                        //repeat:1
                        //repeat:2
                        //repeat:1
                        //repeat:2
                    }
                });
    }

    private void doJustTest() {
        Observable
                .just(new int[]{1, 3, 4}, new int[]{7, 4, 7})
                .subscribe(new Consumer<int[]>() {
                    @Override
                    public void accept(int[] ints) throws Exception {
                        StringBuilder stringBuilder = new StringBuilder();
                        for (int num : ints) {
                            stringBuilder.append(num);
                        }
                        Lg.d("just:" + stringBuilder.toString());
                        //just:134
                        //just:747
                    }
                });
    }

    /**
     * 使用interval实现轮询，心跳任务
     * <p>
     * interval返回的固定为Observable<Long>，表示当前轮询次数
     */
    private void doRxText7() {

        final ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add("aaa");
        arrayList.add("bbb");
        arrayList.add("ccc");

        Observable
                //(第一个时间延迟时间，周期，单位)
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

    public Observable<Long> intervalCountdown(final long time) {
        return Observable.interval(1, TimeUnit.SECONDS)
                .map(new Function<Long, Long>() {
                    @Override
                    public Long apply(@NonNull Long aLong) throws Exception {
                        return time - aLong;
                    }
                }).take(time + 1);
    }

    /**
     * 使用zip,合并Observable
     * <p>
     * 最终接收器收到的事件数量是和发送器发送事件最少的那个发送器的发送事件数目相同，
     */
    private void doRxText6() {
        Observable<String> observable1 = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<String> e) throws Exception {
                e.onNext("AAA");
                e.onNext("BBB");
                e.onNext("CCC");
            }
        });

        Observable<Student> observable2 = Observable.create(new ObservableOnSubscribe<Student>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Student> e) throws Exception {
                e.onNext(new Student("111", 1));
                e.onNext(new Student("222", 2));
                e.onNext(new Student("333", 3));
                e.onNext(new Student("444", 4));
                //没有匹配的，不显示
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
                Toast.makeText(Rx2Tutorial1.this, s, Toast.LENGTH_SHORT).show();
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
     * 使用FlatMap，实现连续请求(不能保证事件的顺序,ConcatMap有序)。
     * flatMap 操作符可以将一个发射数据的 Observable "变换为多个" Observables ，
     * 然后将它们发射的数据合并后放到一个单独的 Observable。
     */
    private void doRxText5() {
        initSchoolClassData();
        doFlatMapTestDelay();//延迟测试FlatMap无序
        doConcatMapTestDelay();//ConcatMap有序
        doFlatMapTest1();//遍历SchoolClass获取数据！
        doFlatMapTest2(); //OkHttp请求获取数据
    }

    private void doFlatMapTestDelay() {
        Observable
                .create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(@NonNull ObservableEmitter<Integer> e) throws Exception {
                        e.onNext(1);
                        e.onNext(2);
                        e.onNext(3);
                    }
                })
                .flatMap(new Function<Integer, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(@NonNull Integer integer) throws Exception {
                        List<String> list = new ArrayList<>();
                        for (int i = 0; i < 3; i++) {
                            list.add("I am value " + integer);
                        }
                        //随机生成一个时间
                        int delayTime = (int) (1 + Math.random() * 10);
                        return Observable.fromIterable(list).delay(delayTime, TimeUnit.MILLISECONDS);
                    }
                })
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        Lg.d("flatMap accept: " + s);
                    }
                });
    }

    private void doConcatMapTestDelay() {
        Observable
                .create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(@NonNull ObservableEmitter<Integer> e) throws Exception {
                        e.onNext(1);
                        e.onNext(2);
                        e.onNext(3);
                    }
                })
                .concatMap(new Function<Integer, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(@NonNull Integer integer) throws Exception {
                        List<String> list = new ArrayList<>();
                        for (int i = 0; i < 3; i++) {
                            list.add("I am value " + integer);
                        }
                        //随机生成一个时间
                        int delayTime = (int) (1 + Math.random() * 10);
                        return Observable.fromIterable(list).delay(delayTime, TimeUnit.MILLISECONDS);
                    }
                })
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        Lg.d("concat accept: " + s);
                    }
                });
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
