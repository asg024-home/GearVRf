/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gearvrf;

/**
 * Opaque type that specifies a custom material shader.
 * Currently material and post effect shaders are kept in separate realms.
 * Material shader IDs may overlap PostEffect shader IDs.
 *
 * You get these from {@link GVRMaterialShaderManager#addShader(String, String)}
 * and you can pass them to
 * {@link GVRMaterial#GVRMaterial(GVRContext, GVRMaterialShaderId)},
 * {@link GVRMaterial#setShaderType(GVRShaderId)}, and
 * {@link GVRMaterialShaderManager#getShaderMap(GVRCustomMaterialShaderId)}.
 */
public class GVRCustomMaterialShaderId extends GVRMaterialShaderId {

    GVRCustomMaterialShaderId(int id) {
        super(id);
    }
    GVRCustomMaterialShaderId(GVRShaderId id) { super(id.ID); }
}