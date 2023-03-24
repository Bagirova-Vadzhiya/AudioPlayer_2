package com.example.audioplayer_2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity implements Runnable {

    // создание полей
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private SeekBar seekBar;
    private boolean wasPlaying = false;
    private FloatingActionButton fabPlayPause;
    private TextView timeDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // присваивание полям id ресурсов
        fabPlayPause = findViewById(R.id.fabPlayPause);
        timeDuration = findViewById(R.id.timeDuration);
        seekBar = findViewById(R.id.seekBar);

        // создание слушателя нажатия кнопки fabPlayPause
        fabPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
             playSong(); // воспроизведение музыки
            }
        });
        // создание слушателя изменения SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            // метод при перетаскивании ползунка по шкале,
            // где progress позволяет получить нове значение ползунка (позже progress назрачается длина трека в миллисекундах)
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                timeDuration.setVisibility(View.VISIBLE); // установление видимости timeDuration
                //timeDuration.setVisibility(View.INVISIBLE); // установление не видимости timeDuration

                // Math.ceil() - округление до целого в большую сторону
                int timeTrack = (int) Math.ceil(progress/1000f); // перевод времени из миллисекунд в секунды
                int minute = timeTrack / 60;
                 // вывод на экран времени отсчёта трека
                timeDuration.setText(String.format("%02d",minute) + ":" + String.format("%02d",(timeTrack % 60)));


                // передвижение времени отсчёта трека
                double percentTrack = progress / (double) seekBar.getMax(); // получение процента проигранного трека (проигранное время делится на длину трека)
                // seekBar.getX() - начало seekBar по оси Х
                // seekBar.getWidth() - ширина контейнера seekBar
                // 0.92 - поправочный коэффициент (так как seekBar занимает не всю ширину своего контейнера)
                timeDuration.setX(seekBar.getX() + Math.round(seekBar.getWidth()*percentTrack*0.92));

                if (progress > 0 && mediaPlayer != null && !mediaPlayer.isPlaying()) { // если mediaPlayer не пустой и mediaPlayer не воспроизводится
                    clearMediaPlayer(); // остановка и очистка MediaPlayer
                    // назначение кнопке картинки play
                    fabPlayPause.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_media_play));
                }
            }
            // метод при начале перетаскивания ползунка по шкале
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
              timeDuration.setVisibility(View.INVISIBLE); // установление видимости timeDuration
            }
            // метод при завершении перетаскивания ползунка по шкале
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) { // если mediaPlayer не пустой и mediaPlayer воспроизводится
                    mediaPlayer.seekTo(seekBar.getProgress());
                }
            }
        });
    }

    public void playSong() {
        try { // обработка исключения на случай отстутствия файла
            if (mediaPlayer != null && mediaPlayer.isPlaying()) { // если mediaPlayer не пустой и mediaPlayer воспроизводится
               clearMediaPlayer(); // остановка и очиска MediaPlayer
                wasPlaying = true; // инициализация значения запуска аудио-файла
                // назначение кнопке картинки play
                fabPlayPause.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_media_play));
            }

            if (!wasPlaying) {
                if (mediaPlayer == null) { // если mediaPlayer пустой
                    mediaPlayer = new MediaPlayer(); // то выделяется для него память
                }
                // назначение кнопке картинки pause
                fabPlayPause.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_media_pause));

                // альтернативный способ считывания файла с помощью файлового дескриптора
                AssetFileDescriptor descriptor = getAssets().openFd("Дворжак - Симфония №9 (Из Нового Света) 4 часть.mp3");
                // запись файла в mediaPlayer, задаются параметры (путь файла, смещение относительно начала файла, длина аудио в файле)
                mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
                descriptor.close(); // закрытие дескриптора

                mediaPlayer.prepare(); // ассинхронная подготовка плейера к проигрыванию
                mediaPlayer.setVolume(0.7f, 0.7f); // задание уровня громкости левого и правого динамиков
                mediaPlayer.setLooping(false); // назначение отстутствия повторов
                seekBar.setMax(mediaPlayer.getDuration()); // ограниечение seekBar длинной трека
                mediaPlayer.seekTo(seekBar.getProgress());

                mediaPlayer.start(); // старт mediaPlayer
                new Thread(this).start(); // запуск дополнительного потока
            }

            wasPlaying = false; // возврат отсутствия проигрывания mediaPlayer

        } catch (Exception e) { // обработка исключения на случай отстутствия файла
            e.printStackTrace(); // вывод в консоль сообщения отсутствия файла
        }
    }

    // при уничтожении активности вызов метода остановки и очистки MediaPlayer
    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearMediaPlayer();
    }

    // метод остановки и очистки MediaPlayer
    private void clearMediaPlayer() {
        mediaPlayer.stop(); // остановка медиа
        mediaPlayer.release(); // освобождение ресурсов
        mediaPlayer = null; // обнуление ресурсов
    }

    // метод дополнительного потока для обновления SeekBar
    @Override
    public void run() {
       int currentPosition = mediaPlayer.getCurrentPosition(); // считывание текущей позиции трека
       int total = mediaPlayer.getDuration(); // считывание длины трека

        // бесконечный цикл при установки не нулевого mediaPlayer, проигрывания трека и текущей позиции трека меньше длины трека
        while (mediaPlayer != null && mediaPlayer.isPlaying() && currentPosition < total) {
            try {

                Thread.sleep(1000); // засыпание вспомогательного потока на 1 секунду
                currentPosition = mediaPlayer.getCurrentPosition(); // обновление текущей позиции трека
            } catch (InterruptedException e) { // вызывание в случае блокировки данного потока
                e.printStackTrace();
                return; // выброс из цикла
            } catch (Exception e) {
                return;
            }
            seekBar.setProgress(currentPosition); // обновление seekBar текущей позиции трека
        }
    }
}