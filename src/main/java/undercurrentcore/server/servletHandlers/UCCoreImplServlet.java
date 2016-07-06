package undercurrentcore.server.servletHandlers;

import api.undercurrent.iface.IUCTile;
import api.undercurrent.iface.UCCollection;
import api.undercurrent.iface.UCTileDefinition;
import api.undercurrent.iface.editorTypes.EditorType;
import com.google.common.base.Throwables;
import com.google.gson.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.DimensionManager;
import undercurrentcore.persist.UCBlockDTO;
import undercurrentcore.persist.UCPlayersWorldData;
import undercurrentcore.server.RequestReturnObject;
import undercurrentcore.server.constants.ResponseTypes;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by Niel Verster on 5/26/2015.
 */

public class UCCoreImplServlet extends HttpServlet {

    Gson gson;
    public static Logger logger = Logger.getLogger("UnderCurrentCore");

    public UCCoreImplServlet() {
        gson = new Gson();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setContentType("text/html;charset=utf-8");
        resp.setStatus(HttpServletResponse.SC_OK);


        String secretKey = req.getParameter("secretKey");

        // Request checks
        if (secretKey == null || secretKey.equals("") || secretKey.isEmpty()) {
            RequestReturnObject rro = new RequestReturnObject(false, ResponseTypes.EMPTY_REQUEST_PARAMETER.toString());
            resp.getWriter().write(gson.toJson(rro));
            return;
        }

        UCPlayersWorldData data = (UCPlayersWorldData) MinecraftServer.getServer().getEntityWorld().perWorldStorage.loadData(UCPlayersWorldData.class, UCPlayersWorldData.GLOBAL_TAG);

        if (!data.checkPlayerOnSecretKey(secretKey)) {
            RequestReturnObject rro = new RequestReturnObject(false, ResponseTypes.USER_NOT_REGISTERED.toString());
            resp.getWriter().write(gson.toJson(rro));
            return;
        }

        List<UCBlockDTO> blocks = data.getUCPlayerInfo(secretKey).getBlocks();

        if (blocks == null) {
            RequestReturnObject rro = new RequestReturnObject(false, ResponseTypes.USER_NOT_REGISTERED.toString());
            resp.getWriter().write(gson.toJson(rro));
            return;
        }

        JsonArray blocksToReturn = new JsonArray();
        try {
            for (UCBlockDTO block : blocks) {
                JsonObject blockObject = new JsonObject();
                JsonArray editableFields = new JsonArray();
                blockObject.addProperty("internalName", block.getInternalName());
                blockObject.addProperty("name", block.getName());
                blockObject.addProperty("xCoord", block.getxCoord());
                blockObject.addProperty("yCoord", block.getyCoord());
                blockObject.addProperty("zCoord", block.getzCoord());
                blockObject.addProperty("dim", block.getDim());
                blockObject.addProperty("dimName", DimensionManager.getProvider(block.getDim()).getDimensionName());
                editableFields.add(gson.toJsonTree(block.getInstance().getTileDefinition()));
                blockObject.add("editedData", editableFields);
                blocksToReturn.add(blockObject);
            }
            RequestReturnObject rro = new RequestReturnObject(true, blocksToReturn);
            resp.getWriter().write(gson.toJson(rro));
        } catch (Exception e) {
            RequestReturnObject rro = new RequestReturnObject(false, ResponseTypes.SERVER_ERROR.toString());
            resp.getWriter().write(gson.toJson(rro));
            logger.severe("UnderCurrentCore: Problem while serializing block description objects due to: " + Throwables.getStackTraceAsString(e));
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setContentType("text/html;charset=utf-8");
        resp.setStatus(HttpServletResponse.SC_OK);

        Gson gson = new Gson();

        String secretKey = req.getParameter("secretKey");

        // Request checks
        if (secretKey == null || secretKey.equals("") || secretKey.isEmpty()) {
            RequestReturnObject rro = new RequestReturnObject(false, ResponseTypes.EMPTY_REQUEST_PARAMETER.toString());
            resp.getWriter().write(gson.toJson(rro));
            return;
        }

        UCPlayersWorldData worldData = (UCPlayersWorldData) MinecraftServer.getServer().getEntityWorld().perWorldStorage.loadData(UCPlayersWorldData.class, UCPlayersWorldData.GLOBAL_TAG);

        if (!worldData.checkPlayerOnSecretKey(secretKey)) {
            RequestReturnObject rro = new RequestReturnObject(false, ResponseTypes.USER_NOT_REGISTERED.toString());
            resp.getWriter().write(gson.toJson(rro));
            return;
        }

        JsonParser parser = new JsonParser();
        JsonObject obj = parser.parse(req.getReader()).getAsJsonObject();

        if (!obj.has("data")) {
            RequestReturnObject rro = new RequestReturnObject(false, ResponseTypes.MISSING_BODY_MEMBER.toString() + "::data");
            resp.getWriter().write(gson.toJson(rro));
            return;
        }

        JsonArray data = obj.getAsJsonArray("data");

        for (int i = 0; i < data.size(); i++) {
            JsonObject objectIteratable = data.get(i).getAsJsonObject();

            if (!objectIteratable.has("internalName")) {
                RequestReturnObject rro = new RequestReturnObject(false, ResponseTypes.MISSING_BODY_MEMBER.toString() + "::internalName");
                resp.getWriter().write(gson.toJson(rro));
                return;
            }

            UCBlockDTO blockToUpdate = worldData.getBlockByInternalName(secretKey, objectIteratable.get("internalName").getAsString());

            if (blockToUpdate == null) {
                RequestReturnObject rro = new RequestReturnObject(false, ResponseTypes.NO_BLOCK_FOUND_FOR_INTERNAL_NAME.toString());
                resp.getWriter().write(gson.toJson(rro));
                return;
            }

            JsonObject swapper = new JsonObject();
            TileEntity te = DimensionManager.getWorld(blockToUpdate.getDim()).getTileEntity(blockToUpdate.getxCoord(), blockToUpdate.getyCoord(), blockToUpdate.getzCoord());

            if (te == null) {
                RequestReturnObject rro = new RequestReturnObject(false, ResponseTypes.WORLD_TE_DOES_NOT_EXIST.toString());
                resp.getWriter().write(gson.toJson(rro));
                logger.severe("UnderCurrentCore: Problem locating TE while preparing for swapping. Object: " + blockToUpdate.toString());
                return;
            }

            if (!(te instanceof IUCTile)) {
                RequestReturnObject rro = new RequestReturnObject(false, ResponseTypes.TE_NOT_IUCTILE.toString());
                resp.getWriter().write(gson.toJson(rro));
                return;
            }

            HashSet<String> editableFields = new HashSet<String>();
            UCTileDefinition tileDefinition;

            try {
                tileDefinition = ((IUCTile) te).getTileDefinition();
            } catch (Exception e) {
                RequestReturnObject rro = new RequestReturnObject(false, ResponseTypes.ERROR_GETTING_TILE_UCTILEDEF.toString());
                resp.getWriter().write(gson.toJson(rro));
                return;
            }

            if (tileDefinition == null) {
                RequestReturnObject rro = new RequestReturnObject(false, ResponseTypes.UCTILEDEF_IS_NULL.toString());
                resp.getWriter().write(gson.toJson(rro));
                return;
            }


            if (!objectIteratable.has("editedData")) {
                RequestReturnObject rro = new RequestReturnObject(false, ResponseTypes.MISSING_BODY_MEMBER.toString() + "::editedData");
                resp.getWriter().write(gson.toJson(rro));
                return;
            }

            JsonArray collections = objectIteratable.get("editedData").getAsJsonArray();
            for (int j = 0; j < collections.size(); j++) {
                JsonObject currentIteration = collections.get(j).getAsJsonObject();
                try {

                    if (!currentIteration.has("fieldName")) {
                        RequestReturnObject rro = new RequestReturnObject(false, ResponseTypes.MISSING_BODY_MEMBER.toString() + "::fieldName");
                        resp.getWriter().write(gson.toJson(rro));
                        return;
                    }

                    if (!currentIteration.has("fieldValue")) {
                        RequestReturnObject rro = new RequestReturnObject(false, ResponseTypes.MISSING_BODY_MEMBER.toString() + "::fieldValue");
                        resp.getWriter().write(gson.toJson(rro));
                        return;
                    }

                    for (UCCollection collection : tileDefinition.getCollections()) {
                        for (EditorType editorType : collection.getEditableFields()) {
                            if (editorType.getFieldName().equals(currentIteration.get("fieldName").getAsString())) {
                                if (editorType.validateValue(currentIteration.get("fieldValue"))) {
                                    editableFields.add(currentIteration.get("fieldName").getAsString());
                                } else {
                                    RequestReturnObject rro = new RequestReturnObject(false, ResponseTypes.VALUE_NOT_VALID_FOR_FIELD.toString() + "::" + currentIteration.get("fieldName").getAsString());
                                    resp.getWriter().write(gson.toJson(rro));
                                    return;
                                }
                            }
                        }
                    }

                    if (!editableFields.contains(currentIteration.get("fieldName").getAsString())) {
                        RequestReturnObject rro = new RequestReturnObject(false, ResponseTypes.SPECIFIED_EDITABLEFIELD_NOT_EDITABLE.toString() + "::" + currentIteration.get("fieldName").getAsString());
                        resp.getWriter().write(gson.toJson(rro));
                        return;
                    }

                    swapper.add(currentIteration.get("fieldName").getAsString(), currentIteration.get("fieldValue"));
                } catch (Exception e) {
                    RequestReturnObject rro = new RequestReturnObject(false, ResponseTypes.CANT_USE_FIELD.toString() + ": " + currentIteration.get("fieldName").getAsString());
                    resp.getWriter().write(gson.toJson(rro));
                    logger.severe("UnderCurrentCore: Problem adding field for swapping: " + currentIteration.get("fieldName").getAsString() + "::" + Throwables.getStackTraceAsString(e));
                    return;
                }
            }

            try {
                te.getWorldObj().setTileEntity(blockToUpdate.getxCoord(), blockToUpdate.getyCoord(), blockToUpdate.getzCoord(), gson.fromJson(swapper, te.getClass()));
            } catch (Exception e) {
                RequestReturnObject rro = new RequestReturnObject(false, ResponseTypes.CANT_DO_TE_SWOP.toString());
                resp.getWriter().write(gson.toJson(rro));
                logger.severe("UnderCurrentCore: Problem swapping TE due to: " + Throwables.getStackTraceAsString(e));
                return;
            }
        }

        RequestReturnObject rro = new RequestReturnObject(true);
        resp.getWriter().write(gson.toJson(rro));
    }
}
