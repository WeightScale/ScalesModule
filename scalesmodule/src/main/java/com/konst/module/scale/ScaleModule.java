/**
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.konst.module.scale;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.konst.module.*;
import com.konst.module.bluetooth.BluetoothHandler;
import com.konst.module.bluetooth.BluetoothProcessManager;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Главный класс для работы с весовым модулем. Инициализируем в теле программы. В абстрактных методах используем
 * возвращеные результаты после запуска метода {@link ScaleModule#create(Context, String, BluetoothDevice, Handler, InterfaceResultCallback)}}.
 * Пример:
 * com.kostya.module.ScaleModule scaleModule = new com.kostya.module.ScaleModule("version module");
 * scaleModule.init("bluetooth device");
 * @author Kostya
 */
public class ScaleModule extends Module implements Serializable{
    private static ScaleModule instance;
    private ScaleVersion version;
    private ThreadScalesProcess threadScalesProcess;
    private static final String TAG = ScaleModule.class.getName();
    //private Handler handlerCmd;
    //private RunnableAttach runnableAttach;
    private Thread threadAttach;

    //private BluetoothClientThread bluetoothClientThread;
    //private ThreadWeight threadWeight;
    //private ThreadBatteryTemperature threadBatteryTemperature;
    /** Процент заряда батареи (0-100%). */
    private int battery;
    /** Температура в целсиях. */
    private int temperature;
    /** Погрешность веса автоноль. */
    protected int weightError;
    /** Счётчик автообнуления. */
    private int autoNull;
    /** Время срабатывания авто ноля. */
    protected int timerNull;
    /** Номер версии программы. */
    private int numVersion;
    /** Имя версии программы */
    private final String versionName;
    /** АЦП-фильтр (0-15). */
    private int filterADC;
    /** Время выключения весов. */
    private int timeOff;
    /** Скорость порта. */
    private int speedPort;
    private String spreadsheet;
    private String username;
    private String password;
    private String phone;
    /** Калибровочный коэффициент a. */
    private float coefficientA;
    /** Калибровочный коэффициент b. */
    private float coefficientB;
    /** Текущее показание датчика веса. */
    private int sensorTenzo;
    /** Максимальное показание датчика. */
    private int limitTenzo;
    /** Предельный вес взвешивания. */
    private int weightMargin;
    /** Делитель для авто ноль. */
    private static final int DIVIDER_AUTO_NULL = 3;
    /** Флаг использования авто обнуленияю. */
    private boolean enableAutoNull = true;

    /** Имя таблици google disk. */
    public String getSpreadsheet() {
        return spreadsheet;
    }

    public void setSpreadsheet(String spreadsheet) {
        this.spreadsheet = spreadsheet;
    }

    /** Имя акаунта google.*/
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public void write(String command) {
        bluetoothProcessManager.write(command);
        //threadScaleAttach.write(command);
    }

    @Override
    public ObjectCommand sendCommand(Commands commands) {
        return bluetoothProcessManager.sendCommand(commands);
        //return threadScaleAttach.sendCommand(commands);
    }

    /** Константы результата взвешивания. */
    public enum ResultWeight {
        /** Значение веса неправильное. */
        WEIGHT_ERROR,
        /** Значение веса в диапазоне весового модуля. */
        WEIGHT_NORMAL,
        /** Значение веса в диапазоне лилита взвешивания. */
        WEIGHT_LIMIT,
        /** Значение веса в диапазоне перегрузки. */
        WEIGHT_MARGIN
    }

    /** Обратный вызов результата измерения веса. */
    /*public interface WeightCallback{
        *//** Результат веса.
         * @param what Статус веса {@link ResultWeight}
         * @param weight Значение веса в килограммах.
         * @param sensor Значение датчика веса.
         *//*
        void weight(ResultWeight what, int weight, int sensor);
    }*/

    /** Обратный вызов результат измерения батареи и температуры модуля. */
    /*public interface BatteryTemperatureCallback{
        *//** Результат измерения.
         * @param battery Значение заряда батареи модуля в процентах.
         * @param temperature Значение температуры модуля в градусах целсия.
         *//*
        void batteryTemperature(int battery, int temperature);
    }*/

    /** Конструктор класса весового модуля.
     * @param moduleVersion Имя и номер версии в формате [[Имя][Номер]].
     * @throws Exception Ошибка при создании модуля.
     */
    private ScaleModule(Context context, String moduleVersion, BluetoothDevice device/*, InterfaceResultCallback event*/) throws Exception, ErrorDeviceException {
        super(context, device/*, event*/);
        versionName = moduleVersion;
        attach();
    }

    private ScaleModule(Context context, String moduleVersion, String device/*, InterfaceResultCallback event*/) throws Exception, ErrorDeviceException {
        super(context, device/*, event*/);
        versionName = moduleVersion;
        attach();
    }

    private ScaleModule(Context context, String moduleVersion, WifiManager wifiManager/*, InterfaceResultCallback event*/)throws Exception, ErrorDeviceException {
        super(context, wifiManager/*, event*/);
        versionName = moduleVersion;
        attachWiFi();
    }

    public static void create(Context context, String moduleVersion, BluetoothDevice device/*, InterfaceResultCallback event*/) throws Exception, ErrorDeviceException {
        instance = new ScaleModule(context, moduleVersion, device/*, event*/);
    }

    public static void create(Context context, String moduleVersion, BluetoothDevice device, Handler handler/*,InterfaceResultCallback event*/) throws Exception, ErrorDeviceException {
        instance = new ScaleModule(context, moduleVersion, device/*, event*/);
    }

    public static void create(Context context, String moduleVersion, String device, Handler handler/*,InterfaceResultCallback event*/) throws Exception, ErrorDeviceException {
        instance = new ScaleModule(context, moduleVersion, device/*, event*/);
    }

    public static void create(Context context, String moduleVersion, String bluetoothDevice/*, InterfaceResultCallback event*/) throws Exception, ErrorDeviceException {
        instance = new ScaleModule(context, moduleVersion, bluetoothDevice/*, event*/);
    }

    public static void createWiFi(Context context, String moduleVersion/*, InterfaceResultCallback event*/) throws Exception, ErrorDeviceException {
        instance = new ScaleModule(context, moduleVersion, (WifiManager)context.getSystemService(Context.WIFI_SERVICE)/*, event*/);
    }

    public static ScaleModule getInstance() { return instance; }

    /** Соединится с модулем. */
    @Override
    public void attach() /*throws InterruptedException*/ {
        if (threadAttach !=null){
            threadAttach.interrupt();
        }
        try {
            threadAttach = new Thread(new RunnableAttach());
            threadAttach.start();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /** Соединится с модулем. */
    @Override
    public void reconnect() /*throws InterruptedException*/ {
        if (threadAttach !=null){
            threadAttach.interrupt();
        }
        try {
            threadAttach = new Thread(new RunnableConnect());
            threadAttach.start();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    protected void load() {
        try {
            version.load();
            getContext().sendBroadcast(new Intent(InterfaceModule.ACTION_LOAD_OK).putExtra(InterfaceModule.EXTRA_MODULE, new ObjectScales()));
            //resultCallback.resultConnect(ResultConnect.STATUS_LOAD_OK, "",instance);
        }  catch (ErrorTerminalException e) {
            getContext().sendBroadcast(new Intent(InterfaceModule.ACTION_TERMINAL_ERROR).putExtra(InterfaceModule.EXTRA_MODULE, new ObjectScales()));
            //resultCallback.resultConnect(ResultConnect.TERMINAL_ERROR, e.getMessage(), instance);
        } catch (Exception e) {
            getContext().sendBroadcast(new Intent(InterfaceModule.ACTION_MODULE_ERROR).putExtra(InterfaceModule.EXTRA_MODULE, new ObjectScales()));
            //resultCallback.resultConnect(ResultConnect.MODULE_ERROR, e.getMessage(), instance);
        }
    }

    @Override
    protected void attachWiFi() throws InterruptedException {
        //new RunnableScaleAttachWiFi();
    }

    /** Определяем после соединения это весовой модуль и какой версии.
     * Проверяем версию указаной при инициализации класса com.kostya.module.ScaleModule.
     * @return true - Версия правильная.
     */
    @Override
    public boolean isVersion() {
        String vrs = getModuleVersion(); //Получаем версию весов
        if (vrs.startsWith(versionName)) {
            try {
                String s = vrs.replace(versionName, "");
                numVersion = Integer.valueOf(s);
                //setVersion(fetchVersion(numVersion));
                version = fetchVersion(numVersion);
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        return false;
    }

    /** Отсоединение от весового модуля.
     * Необходимо использовать перед закрытием программы чтобы остановить работающие процессы
     */
    @Override
    public void dettach() {
        isAttach = false;
        //stopMeasuringWeight();
        //stopMeasuringBatteryTemperature();
        //disconnect();
        if (bluetoothProcessManager != null){
            bluetoothProcessManager.stopProcess();
        }
        /*if (threadScaleAttach != null){
            threadScaleAttach.interrupt();
            threadScaleAttach.cancel();
        }*/
        //version = null;
    }

    @Override
    protected void connectWiFi() throws IOException, NullPointerException {
        WifiInfo info = wifiManager.getConnectionInfo();
        int ip = info.getIpAddress();

        try {
            //Socket socket = new Socket("192.168.4.1", 1001);
            Socket socket = new Socket();
            InetSocketAddress socketAddress = new InetSocketAddress("192.168.4.1", 1001);
            socket.connect(socketAddress, 10000);
            DataOutputStream DataOut = new DataOutputStream(socket.getOutputStream());
            DataOut.writeBytes("Test");
            DataOut.flush();

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /** Разсоединение с bluetooth. */
    /*@Override
    public void disconnect() {
        try {
            if(inputStream != null)
                inputStream.close();
            if(outputStream != null)
                outputStream.close();
            *//*if(bufferedReader != null)
                bufferedReader.close();
            if(bufferedWriter != null)
                bufferedWriter.close();*//*
            if (socket != null)
                socket.close();
        } catch (IOException ioe) {
            socket = null;
        }
        //bufferedReader =  null;
        //bufferedWriter = null;
        outputStream = null;
        inputStream = null;
        socket = null;
    }*/

    /** Определяем версию весов.
     * @param version Имя версии.
     * @return Экземпляр версии.
     * @throws Exception
     */
    private ScaleVersion fetchVersion(int version) throws Exception {
        switch (version) {
            case 1:
                return new ScaleVersion1(this);
            case 4:
                return new ScaleVersion4(this);
            default:
                throw new Exception("illegal version");
        }
    }

    /** Получаем класс загруженой версии весового модуля.
     * @return класс версии весового модуля.
     */
    public ScaleVersion getVersion() {
        return version;
    }

    /** Получаем значение веса погрешности для расчета атоноль.
     * @return возвращяет значение веса.
     */
    public int getWeightError() {
        return weightError;
    }

    /** Сохраняем значение веса погрешности для расчета автоноль.
     * @param weightError Значение погрешности в килограмах.
     */
    public void setWeightError(int weightError) {
        this.weightError = weightError;
    }

    /** Время для срабатывания автоноль.
     * @return возвращяем время после которого установливается автоноль.
     */
    public int getTimerNull() {
        return timerNull;
    }

    /** Устонавливаем значение времени после которого срабатывает автоноль.
     * @param timerNull Значение времени в секундах.
     */
    public void setTimerNull(int timerNull) {
        this.timerNull = timerNull;
    }

    /** Получить номер версии программы.
     * @return Номер версии.  */
    public int getNumVersion() { return numVersion; }

    /** Получить имя акаунта google.
     * @return Имя акаунта. */
    public String getUserName() { return getUsername(); }

    /** Получить максиматьное значение датчика.
     * @return Значение датчика. */
    public int getLimitTenzo(){ return limitTenzo; }

    /** Установить максимальное значение датчика.
     * @param limitTenzo Значение датчика.     */
    public void setLimitTenzo(int limitTenzo) {
        this.limitTenzo = limitTenzo;
    }

    /** Получить температуру модуля.
     * @return Значение температуры. */
    public int getTemperature() {return temperature; }

    /** Получить сохраненое значение фильтраАЦП.
     * @return Значение фильтра от 1 до 15.   */
    public int getFilterADC() {
        return filterADC;
    }

    /** Установить значение фильтра АЦП.
     * @param filterADC Значение АЦП.*/
    public void setFilterADC(int filterADC) {
        this.filterADC = filterADC;
    }

    /** Получить коэффициент каллибровки.
     * @return Значение коэффициента. */
    public float getCoefficientA() { return coefficientA; }

    /** Усттановить коэффициент каллибровки (только локально не в модуле).
     * @param coefficientA Значение коэффициента.     */
    public void setCoefficientA(float coefficientA) {
        this.coefficientA = coefficientA;
    }

    /** Получить коэффициент смещения.
     * @return Значение коэффициента.  */
    public float getCoefficientB() {
        return coefficientB;
    }

    /** Усттановить коэффициент смещения (только локально не в модуле).
     * @param coefficientB Значение коэффициента.     */
    public void setCoefficientB(float coefficientB) {
        this.coefficientB = coefficientB;
    }

    /** Пароль акаунта google.*/ /** Получить пароль акаута google.
     * @return Пароль.   */
    public String getPassword() {
        return password;
    }

    /** Номер телефона. */ /** Получить номер телефона.
     * @return Номер телефона.   */
    public String getPhone() {
        return phone;
    }


    /** Получить время работы при бездействии модуля.
     * @return Время в минутах.  */
    public int getTimeOff() {
        return timeOff;
    }

    /** Установить время бездействия модуля.
     * @param timeOff Время в минутах.
     */
    public void setTimeOff(int timeOff) {
        this.timeOff = timeOff;
    }

    public String getSpreadSheet() {
        return getSpreadsheet();
    }

    public void setSensorTenzo(int sensorTenzo) {
        this.sensorTenzo = sensorTenzo;
    }

    public int getSensorTenzo() {
        return sensorTenzo;
    }

    public int getWeightMargin() {
        return weightMargin;
    }

    public void setWeightMargin(int weightMargin) {
        this.weightMargin = weightMargin;
    }

    public int getSpeedPort() {
        return speedPort;
    }

    public void setSpeedPort(int speedPort) {
        this.speedPort = speedPort;
    }

    //==================================================================================================================

    /** Установливаем сервис код.
     *
     * @param cod Код.
     * @return true Значение установлено.
     * @see Commands#SRC
     */
    public boolean setModuleServiceCod(String cod) {
        return Commands.SRC.setParam(cod);
    }

    /** Получаем сервис код.
     * @return код
     * @see Commands#SRC
     */
    public String getModuleServiceCod() {
        return Commands.SRC.getParam();
        //return cmd(InterfaceVersions.CMD_SERVICE_COD);
    }

    public String getAddressBluetoothDevice() { return getDevice().getAddress(); }

    public int getMarginTenzo() {
        return version.getMarginTenzo(); }

    public int getWeightMax(){
        return version.getWeightMax(); }

    /** Установливаем новое значение АЦП в весовом модуле. Знчение от1 до 15.
     * @param filterADC Значение АЦП от 1 до 15.
     * @return true Значение установлено.
     * @see Commands#FAD
     */
    public boolean setModuleFilterADC(int filterADC) {
        if(Commands.FAD.setParam(filterADC)){
            this.filterADC = filterADC;
            return true;
        }
        return false;
    }

    /** Получаем из весового модуля время выключения при бездействии устройства.
     * @return время в минутах.
     * @see Commands#CMD_TIMER
     */
    /*public String getModuleTimeOff() {
        return Commands.CMD_TIMER.getParam();
    }*/

    /** Записываем в весовой модуль время бездействия устройства.
     * По истечению времени модуль выключается.
     * @param timeOff Время в минутах.
     * @return true Значение установлено.
     * @see Commands#TOF
     */
    public boolean setModuleTimeOff(int timeOff) {
        if(Commands.TOF.setParam(timeOff)){
            this.timeOff = timeOff;
            return true;
        }
        return false;
    }

    /** Получаем значение скорости порта bluetooth модуля обмена данными.
     * Значение от 1 до 5.
     * 1 - 9600bps.
     * 2 - 19200bps.
     * 3 - 38400bps.
     * 4 - 57600bps.
     * 5 - 115200bps.
     *
     * @return Значение от 1 до 5.
     * @see Commands#BST
     */
    public String getModuleSpeedPort() {
        return Commands.BST.getParam();
    }

    /** Устанавливаем скорость порта обмена данными bluetooth модуля.
     * Значение от 1 до 5.
     * 1 - 9600bps.
     * 2 - 19200bps.
     * 3 - 38400bps.
     * 4 - 57600bps.
     * 5 - 115200bps.
     *
     * @param speed Значение скорости.
     * @return true - Значение записано.
     * @see Commands#BST
     */
    public boolean setModuleSpeedPort(int speed) {
        return Commands.BST.setParam(speed);
    }

    /** Получить офсет датчика веса.
     * @return Значение офсет.
     * @see Commands#GCO
     */
    public String getModuleOffsetSensor() {
        return Commands.GCO.getParam();
    }

    /** Получить значение датчика веса.
     * @return Значение датчика.
     * @see Commands#DCH
     */
    public String feelWeightSensor() {
        return Commands.DCH.getParam();
    }

    /** Получаем значение заряда батерии.
     * @return Заряд батареи в процентах.
     * @see Commands#GBT
     */
    private int getModuleBatteryCharge() {
        try {
            battery = Integer.valueOf(Commands.GBT.getParam());
        } catch (Exception e) {
            battery = -0;
        }
        return battery;
    }

    /** Устанавливаем заряд батареи.
     * Используется для калибровки заряда батареи.
     * @param charge Заряд батереи в процентах.
     * @return true - Заряд установлен.
     * @see Commands#CBT
     */
    public boolean setModuleBatteryCharge(int charge) {
        if(Commands.CBT.setParam(charge)){
            battery = charge;
            return true;
        }
        return false;
    }

    /** Получаем значение температуры весового модуля.
     * @return Температура в градусах.
     * @see Commands#DTM
     */
    private int getModuleTemperature() {
        try {
            int temp = Integer.valueOf(Commands.DTM.getParam());
            return (int) ((double) (float) (( temp - 0x800000) / 7169) / 0.81) - 273;
        } catch (Exception e) {
            return -273;
        }
    }

    /** Устанавливаем имя весового модуля.
     * @param name Имя весового модуля.
     * @return true - Имя записано в модуль.
     * @see Commands#SNA
     */
    public boolean setModuleName(String name) { return Commands.SNA.setParam(name);}

    /** Устанавливаем калибровку батареи.
     * @param percent Значение калибровки в процентах.
     * @return true - Калибровка прошла успешно.
     * @see Commands#CBT
     */
    public boolean setModuleCalibrateBattery(int percent) {
        return Commands.CBT.setParam(percent);
    }

    /** Установить обнуление.
     * @return true - Обнуление установлено.
     */
    public synchronized boolean setOffsetScale() {
        return version.setOffsetScale();
    }

    /** Устанавливаем имя spreadsheet google drive в модуле.
     * @param sheet Имя таблици.
     * @return true - Имя записано успешно.
     */
    public boolean setModuleSpreadsheet(String sheet) {
        if (Commands.SGD.setParam(sheet)){
            setSpreadsheet(sheet);
            return true;
        }
        return false;
    }

    /** Устанавливаем имя аккаунта google в модуле.
     * @param username Имя аккаунта.
     * @return true - Имя записано успешно.
     */
    public boolean setModuleUserName(String username) {
        if (Commands.UGD.setParam(username)){
            this.setUsername(username);
            return true;
        }
        return false;
    }

    /** Устанавливаем пароль в google.
     * @param password Пароль аккаунта.
     * @return true - Пароль записано успешно.
     */
    public boolean setModulePassword(String password) {
        if (Commands.PGD.setParam(password)){
            this.setPassword(password);
            return true;
        }
        return false;
    }

    /** Устанавливаем номер телефона. Формат "+38хххххххххх".
     * @param phone Номер телефона.
     * @return true - телефон записано успешно.
     */
    public boolean setModulePhone(String phone) {
        if(Commands.PHN.setParam(phone)){
            this.setPhone(phone);
            return true;
        }
        return false;
    }

    /** Выключить питание модуля.
     * @return true - питание модкля выключено.
     */
    public boolean powerOff() {
        return Commands.POF.getParam().equals(Commands.POF.getName());
    }

    public void setWeightMax(int weightMax) {
        version.setWeightMax(weightMax);
    }

    public void setEnableAutoNull(boolean enableAutoNull) {this.enableAutoNull = enableAutoNull;}

    public boolean writeData() {
        return version.writeData();
    }

    public void resetAutoNull(){ autoNull = 0; }

    /*private class RunnableScaleAttachWiFi implements Runnable {
        final Thread thread;

        RunnableScaleAttachWiFi(){
            resultCallback.resultConnect(ResultConnect.STATUS_ATTACH_START, "WiFi", null);
            thread = new Thread(this);
            thread.start();
        }

        @Override
        public void run() {
            try {
                connectWiFi();
                *//*if (isVersion()) {
                    try {
                        version.load();
                        connectResultCallback.resultConnect(ResultConnect.STATUS_LOAD_OK, "",instance);
                    } catch (ErrorModuleException e) {
                        //connectResultCallback.connectError(ResultError.MODULE_ERROR, e.getMessage());
                        connectResultCallback.resultConnect(ResultConnect.MODULE_ERROR, e.getMessage(), instance);
                    } catch (ErrorTerminalException e) {
                        //connectResultCallback.connectError(ResultError.TERMINAL_ERROR, e.getMessage());
                        connectResultCallback.resultConnect(ResultConnect.TERMINAL_ERROR, e.getMessage(), instance);
                    } catch (Exception e) {
                        //connectResultCallback.connectError(ResultError.MODULE_ERROR, e.getMessage());
                        connectResultCallback.resultConnect(ResultConnect.MODULE_ERROR, e.getMessage(), instance);
                    }
                } else {
                    disconnect();
                    connectResultCallback.resultConnect(ResultConnect.STATUS_VERSION_UNKNOWN, "", null);
                }*//*
            } catch (IOException e) {
                //connectResultCallback.connectError(ResultError.CONNECT_ERROR, e.getMessage());
                resultCallback.resultConnect(ResultConnect.CONNECT_ERROR, e.getMessage(), null);

            }
            //connectResultCallback.resultConnect(ResultConnect.STATUS_ATTACH_FINISH, "");
            resultCallback.resultConnect(ResultConnect.STATUS_ATTACH_FINISH, "", null);
        }
    }*/

    private class ThreadScalesProcess extends Thread{
        private final ObjectScales objectScales;
        private int numTimeTemp = 101;
        private boolean cancel;
        private static final int PERIOD_UPDATE = 20;

        ThreadScalesProcess(){
            objectScales = new ObjectScales();
        }

        @Override
        public void run() {
            while (!interrupted() && !cancel){
                try{
                    objectScales.setWeight(version.updateWeight());
                    ResultWeight resultWeight;
                    if (objectScales.getWeight() == Integer.MIN_VALUE) {
                        resultWeight = ResultWeight.WEIGHT_ERROR;
                    } else {
                        if (version.isLimit())
                            resultWeight = version.isMargin() ? ResultWeight.WEIGHT_MARGIN : ResultWeight.WEIGHT_LIMIT;
                        else {
                            resultWeight = ResultWeight.WEIGHT_NORMAL;
                        }
                    }
                    objectScales.setTenzoSensor(version.getSensor());

                    if (numTimeTemp > 100){
                        numTimeTemp = 0;
                        objectScales.setBattery(getModuleBatteryCharge());
                        objectScales.setTemperature(getModuleTemperature());
                        if (enableAutoNull){
                            if (version.getWeight() != Integer.MIN_VALUE && Math.abs(version.getWeight()) < weightError) { //автоноль
                                autoNull += 1;
                                if (autoNull > timerNull / DIVIDER_AUTO_NULL) {
                                    setOffsetScale();
                                    autoNull = 0;
                                }
                            } else {
                                autoNull = 0;
                            }
                        }
                    }
                    getContext().sendBroadcast(new Intent(InterfaceModule.ACTION_SCALES_RESULT).putExtra(InterfaceModule.EXTRA_SCALES, objectScales));
                    //resultCallback.eventData(resultWeight, objectScales);
                }catch (Exception e){}
                numTimeTemp++;
                try { TimeUnit.MILLISECONDS.sleep(PERIOD_UPDATE); } catch (InterruptedException e) {}
            }
            Log.i(TAG, "interrupt");
        }

        @Override
        public void interrupt() {
            super.interrupt();
            cancel = true;
        }
    }

    public void startProcess(){
        stopProcess();
        try {
            threadScalesProcess = new ThreadScalesProcess();
            threadScalesProcess.start();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void stopProcess(){
        try {
            //if (threadScalesProcess!=null)
                //disconnect();

            if (threadScalesProcess != null)
                threadScalesProcess.interrupt();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    void managerConnect(Socket socket){
        if (bluetoothProcessManager != null)
            bluetoothProcessManager.connect();
    }

}
