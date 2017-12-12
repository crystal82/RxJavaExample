package com.knight.jone.rxjava;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GrammarTest extends AppCompatActivity {

    @BindView(R.id.btn_text1)
    Button mBtn_text1;

    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grammar_test);
        ButterKnife.bind(this);
        client = new OkHttpClient();
    }

    @OnClick({R.id.btn_text1, R.id.btn_text2, R.id.btn_text3})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_text1:
                doRxText1();//基础subscriber,observer,subscribe使用
                break;

            case R.id.btn_text2:
                doRxText2();//切换线程subscribeOn,observeOn使用
                break;

            case R.id.btn_text3:
                doRxText3();
                break;
        }
    }

    /**
     * map的使用，结合OkHttp
     *
     */
    private void doRxText3() {
        Observable.create(new ObservableOnSubscribe<Response>() {
            @Override
            public void subscribe(@NonNull final ObservableEmitter<Response> observableEmitter) throws Exception {
                Log.d("asd", "------subscribe-----");
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
                        observableEmitter.onComplete();//访问结束
                    }
                });
            }
        }).map(new Function<Response, String>() {
            @Override
            public String apply(@NonNull Response response) throws Exception {
                return response.body().string();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        //相当于Observe中的next
                        System.out.println("----doOnNext：" + s + "---执行操作-");
                    }
                })
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        System.out.println("----成功：" + s + "----");
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        System.out.println("----失败：" + throwable.toString() + "----");
                    }
                });
    }

    /**
     * 测试线程切换
     * 1、subscribeOn --- subscribe的线程(事件发生地)
     * 2、observerOn --- observer线程（事件处理地）
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
                System.out.println("---RxJava2---subscribe" + Thread.currentThread() + "----");
                e.onNext("测试线程");
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        System.out.println("----RxJava2----onSubscribe:" + Thread.currentThread());
                    }

                    @Override
                    public void onNext(@NonNull String s) {
                        System.out.println("----RxJava2----onNext:" + Thread.currentThread());
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        System.out.println("----RxJava2----onError:" + Thread.currentThread());
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("----RxJava2----onComplete:" + Thread.currentThread());
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
                System.out.println("----subscribe" + e.toString() + "----");
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
                             System.out.println("----RxJava----onSubscribe: 开始前准备" + d.toString());
                             mDisposable = d; //用于停止订阅
                         }

                         @Override
                         public void onNext(@NonNull String s) {
                             System.out.println("----RxJava----onNext：" + s);
                             if ("stop".equals(s)) {
                                 mDisposable.dispose();
                             }
                         }

                         @Override
                         public void onError(@NonNull Throwable e) {
                             System.out.println("----RxJava----onError:" + e.toString());
                         }

                         @Override
                         public void onComplete() {
                             System.out.println("----RxJava----onComplete");
                         }
                     });

    }
}
