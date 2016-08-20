package com.konst.module.scale;

import com.konst.module.Module;

/**
 * @author Kostya on 25.07.2016.
 */
public interface InterfaceCallbackScales {
    /** Результат веса.
     * @param what Статус веса {@link ScaleModule.ResultWeight}
     * @param obj Обьект весов с данными.
     */
    void onCallback(/*ScaleModule.ResultWeight what, */Module obj);

}
